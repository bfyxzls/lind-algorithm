package com.lind.architecture.transport;

import com.lind.architecture.json.JsonMapper;
import com.lind.architecture.json.JsonWritable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 知识点 (2)：教学用 Transport——内存文档库，证明「上层 API 只认 Endpoint，不认 HTTP」。
 */
public final class InMemoryTransport implements Transport {

	public record Call(String endpointId, HttpMethod method, String path, String bodyJson) {
	}

	private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();

	private final List<Call> calls = new CopyOnWriteArrayList<>();

	public List<Call> calls() {
		return List.copyOf(calls);
	}

	public void clear() {
		store.clear();
		calls.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Req, Res> Res performRequest(Endpoint<Req, Res> endpoint, Req request, JsonMapper mapper)
			throws TransportException {
		Objects.requireNonNull(endpoint, "endpoint");
		Objects.requireNonNull(mapper, "mapper");
		try {
			String path = endpoint.path(request);
			String bodyJson = null;
			Object bodyTree = null;
			if (endpoint.hasRequestBody() && request != null) {
				if (request instanceof JsonWritable jw) {
					bodyTree = jw.toJsonTree();
					bodyJson = mapper.writeToString(bodyTree);
				}
				else {
					bodyJson = mapper.writeToString(request);
					bodyTree = mapper.read(bodyJson, Map.class);
				}
			}
			calls.add(new Call(endpoint.id(), endpoint.method(), path, bodyJson));

			Object responseTree = dispatch(endpoint.id(), path, bodyTree);
			String responseJson = mapper.writeToString(responseTree);
			return mapper.read(responseJson, endpoint.responseType());
		}
		catch (TransportException e) {
			throw e;
		}
		catch (Exception e) {
			throw new TransportException("transport failed for " + endpoint.id(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private Object dispatch(String endpointId, String path, Object bodyTree) throws TransportException {
		return switch (endpointId) {
			case "documents.index" -> indexDoc(path, bodyTree);
			case "documents.get" -> getDoc(path);
			case "search" -> search(path, bodyTree);
			case "cluster.health" -> Map.of("status", "green", "cluster_name", "toy");
			default -> throw new TransportException("unknown endpoint: " + endpointId);
		};
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> indexDoc(String path, Object bodyTree) {
		// /{index}/_doc/{id}
		String[] parts = path.split("/");
		String index = parts[1];
		String id = parts[3];
		Map<String, Object> doc = bodyTree instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m)
				: Map.of("value", bodyTree);
		store.computeIfAbsent(index, k -> new ConcurrentHashMap<>()).put(id, doc);
		return Map.of("index", index, "id", id, "result", "created");
	}

	private Map<String, Object> getDoc(String path) throws TransportException {
		String[] parts = path.split("/");
		String index = parts[1];
		String id = parts[3];
		Map<String, Object> idx = store.get(index);
		if (idx == null || !idx.containsKey(id)) {
			throw new TransportException("document not found: " + path);
		}
		return Map.of("index", index, "id", id, "found", true, "source", idx.get(id));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> search(String path, Object bodyTree) {
		String index = path.split("/")[1];
		Map<String, Object> idx = store.getOrDefault(index, Map.of());
		Map<String, Object> query = bodyTree instanceof Map<?, ?> m ? (Map<String, Object>) ((Map<?, ?>) m).get("query")
				: null;
		List<Map<String, Object>> hits = new ArrayList<>();
		for (Map.Entry<String, Object> e : idx.entrySet()) {
			Map<String, Object> source = (Map<String, Object>) e.getValue();
			if (matches(query, source)) {
				hits.add(Map.of("id", e.getKey(), "score", 1.0, "source", source));
			}
		}
		return Map.of("took", 1, "hits", Map.of("total", hits.size(), "hits", hits));
	}

	@SuppressWarnings("unchecked")
	private static boolean matches(Map<String, Object> query, Map<String, Object> source) {
		if (query == null || query.isEmpty()) {
			return true;
		}
		if (query.containsKey("term")) {
			Map<String, Object> term = (Map<String, Object>) query.get("term");
			String field = String.valueOf(term.get("field"));
			Object value = term.get("value");
			return Objects.equals(String.valueOf(source.get(field)), String.valueOf(value));
		}
		if (query.containsKey("match")) {
			Map<String, Object> match = (Map<String, Object>) query.get("match");
			String field = String.valueOf(match.get("field"));
			String value = String.valueOf(match.get("query")).toLowerCase();
			Object raw = source.get(field);
			return raw != null && String.valueOf(raw).toLowerCase().contains(value);
		}
		if (query.containsKey("bool")) {
			Map<String, Object> bool = (Map<String, Object>) query.get("bool");
			List<Map<String, Object>> must = (List<Map<String, Object>>) bool.getOrDefault("must", List.of());
			for (Map<String, Object> q : must) {
				if (!matches(q, source)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

}
