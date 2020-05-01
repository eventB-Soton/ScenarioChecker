package ac.soton.eventb.internal.scenariochecker;

/**
 *  Triplet class
 *  
 * @author cfsnook  - copied from https://www.techiedelight.com/implement-3-tuple-triplet-java/
 *
 * @param <U>
 * @param <V>
 * @param <T>
 */
public class Triplet<U, V, T>
{
    public final U first;       // first field of a Triplet
    public final V second;      // second field of a Triplet
    public final T third;       // third field of a Triplet
 
    // Constructs a new Triplet with the given values
    private Triplet(U first, V second, T third)
    {
        this.first = first;
        this.second = second;
        this.third = third;
    }
 
    @SuppressWarnings("rawtypes")
	@Override
    public boolean equals(Object o)
    {
        /* Checks specified object is "equal to" current object or not */
 
        if (this == o) return true;
 
        if (o == null || getClass() != o.getClass())
            return false;
 
        Triplet triplet = (Triplet) o;
 
        // call equals() method of the underlying objects
        if (!first.equals(triplet.first) ||
                !second.equals(triplet.second) ||
                !third.equals(triplet.third))
            return false;
 
        return true;
    }
 
    @Override
    public int hashCode()
    {
        /* Computes hash code for an object by using hash codes of 
        the underlying objects */
 
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        result = 31 * result + third.hashCode();
        return result;
    }
 
    @Override
    public String toString()
    {
        return "(" + first + ", " + second + ", " + third + ")";
    }
 
    // Factory method for creating a Typed immutable instance of Triplet
    public static <U, V, T> Triplet <U, V, T> of(U a, V b, T c)
    {
        return new Triplet <>(a, b, c);
    }
}
