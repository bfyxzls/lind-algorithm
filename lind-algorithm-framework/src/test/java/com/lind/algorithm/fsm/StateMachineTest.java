package com.lind.algorithm.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * {@link StateMachine} 单元测试（订单流转场景）。
 */
public class StateMachineTest {

	enum OrderState {

		CREATED, PAID, SHIPPED, COMPLETED, CANCELLED

	}

	enum OrderEvent {

		PAY, SHIP, COMPLETE, CANCEL

	}

	static final class OrderContext {

		private boolean cancelable = true;

		private final List<String> trail = new ArrayList<>();

		void mark(String msg) {
			trail.add(msg);
		}

	}

	@Test
	void happyPathOrderFlow() {
		OrderContext ctx = new OrderContext();
		StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
				.<OrderState, OrderEvent, OrderContext>builder().initial(OrderState.CREATED).context(ctx)
				.from(OrderState.CREATED).on(OrderEvent.PAY).action((f, t, e, c) -> c.mark("paid")).to(OrderState.PAID)
				.from(OrderState.PAID).on(OrderEvent.SHIP).to(OrderState.SHIPPED).from(OrderState.SHIPPED)
				.on(OrderEvent.COMPLETE).to(OrderState.COMPLETED).build();

		assertEquals(OrderState.CREATED, sm.getState());
		assertTrue(sm.canFire(OrderEvent.PAY));
		assertFalse(sm.canFire(OrderEvent.SHIP));

		sm.fire(OrderEvent.PAY);
		assertEquals(OrderState.PAID, sm.getState());
		sm.fire(OrderEvent.SHIP);
		assertEquals(OrderState.SHIPPED, sm.getState());
		sm.fire(OrderEvent.COMPLETE);
		assertEquals(OrderState.COMPLETED, sm.getState());
		assertEquals(List.of("paid"), ctx.trail);
	}

	@Test
	void guardRejectsCancelWhenNotAllowed() {
		OrderContext ctx = new OrderContext();
		ctx.cancelable = false;
		StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
				.<OrderState, OrderEvent, OrderContext>builder().initial(OrderState.PAID).context(ctx)
				.from(OrderState.PAID).on(OrderEvent.CANCEL).when(c -> c.cancelable).to(OrderState.CANCELLED)
				.from(OrderState.PAID).on(OrderEvent.SHIP).to(OrderState.SHIPPED).build();

		assertFalse(sm.canFire(OrderEvent.CANCEL));
		assertThrows(TransitionException.class, () -> sm.fire(OrderEvent.CANCEL));
		assertEquals(OrderState.PAID, sm.getState());
		assertTrue(sm.tryFire(OrderEvent.SHIP));
		assertEquals(OrderState.SHIPPED, sm.getState());
	}

	@Test
	void entryAndExitActions() {
		OrderContext ctx = new OrderContext();
		StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
				.<OrderState, OrderEvent, OrderContext>builder().initial(OrderState.CREATED).context(ctx)
				.onExit(OrderState.CREATED, (s, c) -> c.mark("exit-created"))
				.onEntry(OrderState.PAID, (s, c) -> c.mark("enter-paid")).from(OrderState.CREATED).on(OrderEvent.PAY)
				.to(OrderState.PAID).build();

		sm.fire(OrderEvent.PAY);
		assertEquals(List.of("exit-created", "enter-paid"), ctx.trail);
	}

	@Test
	void listenerNotified() {
		AtomicInteger count = new AtomicInteger();
		StateMachine<OrderState, OrderEvent, OrderContext> sm = StateMachine
				.<OrderState, OrderEvent, OrderContext>builder().initial(OrderState.CREATED).context(new OrderContext())
				.from(OrderState.CREATED).on(OrderEvent.PAY).to(OrderState.PAID).build();
		sm.addTransitionListener((te, c) -> {
			assertEquals(OrderState.CREATED, te.from());
			assertEquals(OrderState.PAID, te.to());
			assertEquals(OrderEvent.PAY, te.event());
			count.incrementAndGet();
		});
		sm.fire(OrderEvent.PAY);
		assertEquals(1, count.get());
	}

	@Test
	void illegalTransitionThrows() {
		StateMachine<OrderState, OrderEvent, Void> sm = StateMachine.<OrderState, OrderEvent, Void>builder()
				.initial(OrderState.CREATED).from(OrderState.CREATED).on(OrderEvent.PAY).to(OrderState.PAID).build();

		TransitionException ex = assertThrows(TransitionException.class, () -> sm.fire(OrderEvent.COMPLETE));
		assertEquals(OrderState.CREATED, ex.getFrom());
		assertEquals(OrderEvent.COMPLETE, ex.getEvent());
	}

	@Test
	void builderRequiresInitialState() {
		assertThrows(IllegalStateException.class, () -> StateMachine.<OrderState, OrderEvent, Void>builder()
				.from(OrderState.CREATED).on(OrderEvent.PAY).to(OrderState.PAID).build());
	}

}
