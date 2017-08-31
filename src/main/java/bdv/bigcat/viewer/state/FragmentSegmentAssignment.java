package bdv.bigcat.viewer.state;

import gnu.trove.set.hash.TLongHashSet;

public interface FragmentSegmentAssignment
{

	public long getSegment( final long fragmentId );

	public TLongHashSet getFragments( final long segmentId );

	public void assignFragments( final long segmentId1, final long segmentId2 );

	public void mergeSegments( final long segmentId1, final long segmentId2 );

	public void mergeFragmentSegments( final long fragmentId1, final long fragmentId2 );

	public void detachFragment( final long fragmentId );

}