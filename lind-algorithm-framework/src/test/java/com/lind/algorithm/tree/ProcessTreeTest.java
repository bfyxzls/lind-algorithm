package com.lind.algorithm.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@link ProcessTree} 单元测试。
 */
public class ProcessTreeTest {

	@Test
	void xorChoosesBranchByCondition() {
		ProcessTree.ProcessNode endApprove = ProcessTree.ProcessNode.end("end-ok").build();
		ProcessTree.ProcessNode endReject = ProcessTree.ProcessNode.end("end-no").build();

		ProcessTree.ProcessNode approve = ProcessTree.ProcessNode.task("approve").name("通过")
				.when(ctx -> Boolean.TRUE.equals(ctx.get("ok"))).child(endApprove).build();
		ProcessTree.ProcessNode reject = ProcessTree.ProcessNode.task("reject").name("驳回")
				.when(ctx -> Boolean.FALSE.equals(ctx.get("ok"))).child(endReject).build();

		ProcessTree.ProcessNode xor = ProcessTree.ProcessNode.xor("gateway").children(approve, reject).build();
		ProcessTree.ProcessNode start = ProcessTree.ProcessNode.start("start").child(xor).build();

		ProcessTree tree = new ProcessTree(start);
		assertEquals(List.of("start", "gateway", "approve", "end-ok"), tree.execute(ProcessTree.context("ok", true)));
		assertEquals(List.of("start", "gateway", "reject", "end-no"), tree.execute(ProcessTree.context("ok", false)));
	}

	@Test
	void andRunsAllChildrenAndActions() {
		List<String> log = new ArrayList<>();
		ProcessTree.ProcessNode a = ProcessTree.ProcessNode.task("a").action(ctx -> log.add("a"))
				.child(ProcessTree.ProcessNode.end("end-a").build()).build();
		ProcessTree.ProcessNode b = ProcessTree.ProcessNode.task("b").action(ctx -> log.add("b"))
				.child(ProcessTree.ProcessNode.end("end-b").build()).build();
		ProcessTree.ProcessNode and = ProcessTree.ProcessNode.and("parallel").children(a, b).build();
		ProcessTree tree = new ProcessTree(ProcessTree.ProcessNode.start("s").child(and).build());

		List<String> trace = tree.execute(Map.of());
		assertEquals(List.of("s", "parallel", "a", "end-a", "b", "end-b"), trace);
		assertEquals(List.of("a", "b"), log);
	}

	@Test
	void rootMustBeStart() {
		assertThrows(IllegalArgumentException.class, () -> new ProcessTree(ProcessTree.ProcessNode.task("t").build()));
		assertTrue(ProcessTree.ProcessNode.end("e").build().children().isEmpty());
	}

}
