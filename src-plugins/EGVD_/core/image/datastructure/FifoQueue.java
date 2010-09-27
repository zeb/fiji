package core.image.datastructure;

import java.util.ArrayList;

import core.util.IConstants;


public class FifoQueue {

	private int head;
	private int[] headQueue;
	private int tail;
	private int[] tailQueue;
	private int capacityIncrement;
	private ArrayList<int[]> queues;

	public FifoQueue() {
		this(IConstants.TWO_POWER_15 - 1);
	}

	public FifoQueue(int capacityIncrement) {
		this.capacityIncrement = capacityIncrement;
		queues = new ArrayList<int[]>();
		int[] initialQueue = createNewQueue();
		headQueue = initialQueue;
		tailQueue = initialQueue;
		head = -1;
		tail = -1;
	}

	public void add(int i) {
		head++;
		if (isHeadQueueFull()) {
			headQueue = createNewQueue();
			head = 0;
		}
		headQueue[head] = i;
	}

	public int remove() {
		tail++;
		if (isTailQueueEmpty()) {
			queues.remove(0);
			tailQueue = queues.get(0);
			tail = 0;
		}
		return tailQueue[tail];
	}

	private int[] createNewQueue() {
		int[] queue = new int[capacityIncrement];
		queues.add(queue);
		return queue;
	}

	private boolean isHeadQueueFull() {
		return (head == capacityIncrement);
	}

	private boolean isTailQueueEmpty() {
		return (tail == capacityIncrement);
	}

	public boolean isEmpty() {
		return (head == tail && headQueue == tailQueue);
	}

	public void add(int[] items, int start, int number) {
		if (head + number >= capacityIncrement) {
			for (int i = start; i < start + number; i++) {
				add(items[i]);
			}
		} else {
			System.arraycopy(items, start, headQueue, head + 1, number);
			head += number;
		}
	}
}
