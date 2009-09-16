/*
 * @(#)SortedArrayList.java
 *
 * Summary: Library of methods to operate on sorted ArrayLists.
 *
 * Copyright: (c) 2003-2009 Roedy Green, Canadian Mind Products, http://mindprod.com
 *
 * Licence: This software may be copied and used freely for any purpose but military.
 *          http://mindprod.com/contact/nonmil.html
 *
 * Requires: JDK 1.5+
 *
 * Created with: IntelliJ IDEA IDE.
 *
 * Version History:
 *  1.2 2003-10-06
 *  1.3 2007-07-29 - IntelliJ inspector lint, preparze ANT script,
 *                   icon and pad to prepare for distribution. Make serialisable.
 */
package fiji.recorder.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Library of methods to operate on sorted ArrayLists.
 * <p/>
 * Works with ArrayLists that are kept in order most of the time. You invoke sort any time you wish it sorted by the current
 * Comparator. It is not necessarily in order otherwise. It cleverly avoids sorting whenever the list is already sorted.
 * If you enable assertions with java -ea, then it will warn you if you improperly disturb the order of SortedArrayLists
 * by changing the fields of the ArrayList elements. add and addAll are safe. Otherwise you must invalidate the order.
 * For examples of use, see Merge.main.
 * <p/>
 * @param <E> generic type of the SortedArrayLists, e.g. String
 *
 * @author Roedy Green, Canadian Mind Products
 * @version 1.3 2007-07-29
 * @since 2003-10-03
 */
public final class SortedArrayList<E> extends ArrayList<E> implements Cloneable, Serializable
    {
    // ------------------------------ CONSTANTS ------------------------------

    /**
     * Layout version number.
     */
    public static final long serialVersionUID = 500L;

    /**
     * un-displayed copyright notice
     */
    @SuppressWarnings( {  "unused" } )
    private static final String EMBEDDED_COPYRIGHT =
            "copyright (c) 2003-2009 Roedy Green, Canadian Mind Products, http://mindprod.com";

    @SuppressWarnings( { "unused" } )
    private static final String RELEASE_DATE = "2007-07-29";

    /**
     * name of this application.
     */
    @SuppressWarnings("unused")
	private static final String TITLE_STRING = "Sortedr";

    /* embedded version string, real one is Config.VERSION  */
    @SuppressWarnings( "unused" )
    private static final String VERSION_STRING = "1.3";

    // ------------------------------ FIELDS ------------------------------

    /**
     * Comparator used to sort this list. Possibly null.
     */
    protected Comparator<? super E> comparator;

    /**
     * true if this list is currently sorted in order.
     */
    protected boolean sorted;

    // -------------------------- PUBLIC INSTANCE  METHODS --------------------------

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public SortedArrayList()
        {
        super();
        }

    /**
     * Constructs a list containing the elements of the specified collection, in the order they are returned by the
     * collection's iterator. The <tt>ArrayList</tt> instance has an initial capacity of 110% the size of the specified
     * collection.
     *
     * @param c the collection whose elements are to be placed into this list.
     *
     * @throws NullPointerException if the specified collection is null.
     */
    public SortedArrayList( Collection<E> c )
        {
        super( c );
        }

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list.
     *
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public SortedArrayList( int initialCapacity )
        {
        super( initialCapacity );
        }

    /**
     * Inserts the specified element at the specified position in this list. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     *
     * @param index   index at which the specified element is to be inserted.
     * @param element element to be inserted.
     *
     * @throws IndexOutOfBoundsException if index is out of range <tt>(index &lt; 0 || index &gt; size())</tt>.
     */
    public void add( int index, E element )
        {
        super.add( index, element );
        sorted = false;
        }

    /**
     * Appends all of the elements in the specified Collection to the end of this list, in the order that they are
     * returned by the specified Collection's Iterator. The behavior of this operation is undefined if the specified
     * Collection is modified while the operation is in progress. (This implies that the behavior of this call is
     * undefined if the specified Collection is this list, and this list is nonempty.)
     *
     * @param c the elements to be inserted into this list.
     *
     * @return <tt>true</tt> if this list changed as a result of the call.
     * @throws NullPointerException if the specified collection is null.
     */
    public boolean addAll( Collection<? extends E> c )
        {
        sorted = false;
        return super.addAll( c );
        }

    /**
     * Inserts all of the elements in the specified Collection into this list, starting at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right (increases their
     * indices). The new elements will appear in the list in the order that they are returned by the specified
     * Collection's iterator.
     *
     * @param index index at which to insert first element from the specified collection.
     * @param c     elements to be inserted into this list.
     *
     * @return <tt>true</tt> if this list changed as a result of the call.
     * @throws IndexOutOfBoundsException if index out of range <tt>(index &lt; 0 || index &gt; size())</tt>.
     * @throws NullPointerException      if the specified Collection is null.
     */
    public boolean addAll( int index, Collection<? extends E> c )
        {
        sorted = false;
        return super.addAll( index, c );
        }

    /**
     * Removes all of the elements from this list. The list will be empty after this call returns.
     */
    public void clear()
        {
        super.clear();
        sorted = true;
        }

    /**
     * Clone this object
     *
     * @return a copy of this sortedArrayList.
     */
    @SuppressWarnings( "unchecked" )
    // no ; not javaDoc
    public SortedArrayList<E> clone()
        {
        return ( SortedArrayList<E> ) super.clone();
        }

    /**
     * Get the Comparator for this List.
     *
     * @return Comparator for the list.
     */
    public Comparator<? super E> getComparator()
        {
        return this.comparator;
        }

    /**
     * Set the Comparator for this List without actually sorting yet.
     *
     * @param comparator Comparator to sort by. Null means no particular order.
     */
    public void setComparator( Comparator<? super E> comparator )
        {
        if ( this.comparator == null || comparator == null || !this.comparator
                .equals( comparator ) )
            {
            this.sorted = false;
            }
        this.comparator = comparator;
        }

    /**
     * Invalidate any existing sort. Usually called if you have disturbed the assumed order by changing field in the
     * elements of the SortedArrayList. This forces an actual sort the next elapsedTime sort is called.
     */
    public void invalidate()
        {
        sorted = false;
        }

    /**
     * Is the list currently sorted. Normally you would not bother to check, just call sort. If it is already sorted, it
     * will avoid the work.
     *
     * @return true if the list is currently sorted in order by the current Comparator. If the Comparator is null, this
     *         will always be false;
     */
    public boolean isSorted()
        {
        return sorted && comparator != null;
        }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index   index of element to replace.
     * @param element element to be stored at the specified position.
     *
     * @return the element previously at the specified position.
     * @throws IndexOutOfBoundsException if index out of range <tt>(index &lt; 0 || index &gt;= size())</tt>.
     */
    public E set( int index, E element )
        {
        sorted = false;
        return super.set( index, element );
        }

    /**
     * Sort this list by the current comparator, the one last set by sort. Cleverly avoids sorting if already sorted.
     * Does nothing if the current comparator is null.
     */
    public void sort()
        {
        if ( comparator != null && !sorted )
            {
            Collections.sort( this, comparator );
            sorted = true;
            }
        else
            {
            assert comparator == null
                   || isTrulyInOrder() :
                    "Prexisting order disturbed improperly. Call invalidate() if you change collating fields in sorted elements.";
            }
        }

    /**
     * Sort this list by the given comparator. Cleverly avoids sorting if already sorted.
     *
     * @param comparator Comparator to sort by. Null means no particular order.
     */
    public void sort( Comparator<? super E> comparator )
        {
        setComparator( comparator );
        sort();
        }

    // -------------------------- OTHER METHODS --------------------------

    /**
     * Test that a list truly is in order by the current comparator independent of what the sorted boolean says. Used in
     * debugging.
     *
     * @return true if the list truly is in order. Null comparator returns false.
     */
    private boolean isTrulyInOrder()
        {
        if ( comparator == null )
            {
            return false;
            }
        int size = this.size();
        if ( size <= 1 )
            {
            return true;
            }
        E prev = this.get( 0 );
        for ( int i = 1; i < size; i++ )
            {
            E current = this.get( i );
            if ( comparator.compare( current, prev ) < 0 )
                {
                return false;
                }
            }
        return true;
        }

    // Iterators do not automatically sort first!
    // It would be difficult to implement them since sort uses listIterator.
    }// end SortedArrayList
