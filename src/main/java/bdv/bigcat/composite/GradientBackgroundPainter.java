package bdv.bigcat.composite;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.imglib2.Localizable;
import net.imglib2.type.numeric.ARGBType;

public class GradientBackgroundPainter implements BiConsumer< ARGBType, Localizable >
{

	public static class Factory implements Supplier< BiConsumer< ARGBType, Localizable > >
	{

		private int width;

		private int height;

		public void setDimensions( final int width, final int height )
		{
			this.width = width;
			this.height = height;
		}

		@Override
		public BiConsumer< ARGBType, Localizable > get()
		{
			return new GradientBackgroundPainter( width, height );
		}

	}

	private final double total;

	private final double factor;

	public GradientBackgroundPainter( final int width, final int height )
	{
		super();
		this.total = width * height;
		this.factor = 255.0 / total;
	}

//	private final double max = Math.pow( 2, 16 );

	@Override
	public void accept( final ARGBType t, final Localizable u )
	{
		final double val = factor * ( u.getDoublePosition( 0 ) * u.getDoublePosition( 1 ) );
		t.set( ARGBType.rgba( val, val, val, 255 ) );
	}

}
