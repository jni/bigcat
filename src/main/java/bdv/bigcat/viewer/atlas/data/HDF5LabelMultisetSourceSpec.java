package bdv.bigcat.viewer.atlas.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

import bdv.bigcat.ui.LabelMultisetSource;
import bdv.bigcat.ui.VolatileLabelMultisetSource;
import bdv.bigcat.viewer.state.FragmentSegmentAssignmentState;
import bdv.bigcat.viewer.state.FragmentSegmentAssignmentWithHistory;
import bdv.bigcat.viewer.state.SelectedIds;
import bdv.bigcat.viewer.stream.AbstractHighlightingARGBStream;
import bdv.bigcat.viewer.stream.ModalGoldenAngleSaturatedHighlightingARGBStream;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset.Entry;
import bdv.labels.labelset.VolatileLabelMultisetType;
import bdv.util.volatiles.SharedQueue;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import gnu.trove.map.hash.TLongLongHashMap;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

public class HDF5LabelMultisetSourceSpec implements LabelSpec< LabelMultisetType, VolatileLabelMultisetType >
{

	private final AbstractHighlightingARGBStream stream;

	private final H5LabelMultisetSetupImageLoader loader;

	private final SelectedIds selectedIds = new SelectedIds();

	private final FragmentSegmentAssignmentState< ? > assignment = new FragmentSegmentAssignmentWithHistory( action -> System.out.println( action ), () -> {
		try
		{
			Thread.sleep( 1000 );
		}
		catch ( final InterruptedException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	} );

	private final Consumer< TLongLongHashMap > assignmentWriteBack;

	public HDF5LabelMultisetSourceSpec( final String path, final String dataset, final int[] cellSize ) throws IOException
	{
		this( path, dataset, cellSize, ( asgn ) -> {} );
	}

	public HDF5LabelMultisetSourceSpec( final String path, final String dataset, final int[] cellSize, final Consumer< TLongLongHashMap > assignmentWriteBack ) throws IOException
	{
		super();
		this.stream = new ModalGoldenAngleSaturatedHighlightingARGBStream( selectedIds, assignment );
		final IHDF5Reader h5reader = HDF5Factory.open( path );
		this.loader = new H5LabelMultisetSetupImageLoader( h5reader, null, dataset, 0, cellSize, new VolatileGlobalCellCache( new SharedQueue( 8 ) ) );
		this.assignmentWriteBack = assignmentWriteBack;
	}

	public SelectedIds getSelectedIds()
	{
		return selectedIds;
	}

	@Override
	public LabelMultisetSource getSource()
	{
		final LabelMultisetSource source = new LabelMultisetSource( 0, loader, stream );
		return source;
	}

	@Override
	public VolatileLabelMultisetSource getViewerSource()
	{
		final VolatileLabelMultisetSource source = new VolatileLabelMultisetSource( 0, loader, stream );
		return source;
	}

	@Override
	public Converter< VolatileLabelMultisetType, ARGBType > getViewerConverter()
	{
		return defaultConverter( stream );
	}

	public static Converter< VolatileLabelMultisetType, ARGBType > defaultConverter( final AbstractHighlightingARGBStream stream )
	{
		return new HighlightingStreamConverter( stream );
	}

	public static class HighlightingStreamConverter implements Converter< VolatileLabelMultisetType, ARGBType >
	{

		private final AbstractHighlightingARGBStream stream;

		public HighlightingStreamConverter( final AbstractHighlightingARGBStream stream )
		{
			super();
			this.stream = stream;
		}

		@Override
		public void convert( final VolatileLabelMultisetType input, final ARGBType output )
		{
			final Iterator< Entry< Label > > it = input.get().entrySet().iterator();
			output.set( stream.argb( it.hasNext() ? considerMaxUnsignedInt( it.next().getElement().id() ) : Label.INVALID ) );
		}

		public void setAlpha( final int alpha )
		{
			stream.setAlpha( alpha );
		}

		public void setHighlightAlpha( final int alpha )
		{
			stream.setActiveSegmentAlpha( alpha );
		}

		public void setInvalidSegmentAlpha( final int alpha )
		{
			stream.setInvalidSegmentAlpha( alpha );
		}

		public int getAlpha()
		{
			return stream.getAlpha();
		}

		public int getHighlightAlpha()
		{
			return stream.getActiveSegmentAlpha();
		}

		public int getInvalidSegmentAlpha()
		{
			return stream.getInvalidSegmentAlpha();
		}

		private static long considerMaxUnsignedInt( final long val )
		{
			return val >= Integer.MAX_VALUE ? Label.INVALID : val;
		}

	}

	@Override
	public FragmentSegmentAssignmentState< ? > getAssignment()
	{
		return assignment;
	}

	@Override
	public Consumer< TLongLongHashMap > assignmentWriteBack()
	{
		return this.assignmentWriteBack;
	}

}