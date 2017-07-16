package org.kucro3.collection;

public abstract class LinkedNode<E> implements Sized, MetaCode {
	public void remove()
	{
		if(prev != null)
			prev.next = next;
		if(next != null)
			next.prev = prev;
	}
	
	public void linkBefore(LinkedNode<E> node)
	{
		LinkedNode<E> prev = node.prev;
		node.prev = this;
		if(prev != null)
			prev.next = this;
		this.next = node;
		this.prev = prev;
	}
	
	public void linkAfter(LinkedNode<E> node)
	{
		LinkedNode<E> next = node.next;
		node.next = this;
		if(next != null)
			next.prev = this;
		this.prev = node;
		this.next = next;
	}
	
	public abstract E get(int index);
	
	public int size()
	{
		return 1;
	}
	
	protected E element;
	
	protected LinkedNode<E> prev;
	
	protected LinkedNode<E> next;
}
