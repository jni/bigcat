package bdv.bigcat.viewer.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.cache.CacheControl;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingNode;
import net.imglib2.realtransform.AffineTransform3D;

public class ViewerNode extends SwingNode implements ListChangeListener< SourceAndConverter< ? > >
{

	public enum ViewerAxis
	{
		X, Y, Z
	};

	private final ViewerPanel viewer;

	private final ViewerState state;

	private final ViewerTransformManager manager;

	private ViewerAxis viewerAxis;

	private final HashMap< Source< ? >, Boolean > visibility = new HashMap<>();

	private boolean managesOwnLayerVisibility = false;

	private final InputActionBindings keybindings = new InputActionBindings();

	private final TriggerBehaviourBindings triggerbindings = new TriggerBehaviourBindings();

	private final InputTriggerConfig inputTriggerConfig = new InputTriggerConfig();

	private final Behaviours behaviours = new Behaviours( inputTriggerConfig );

	private final Actions actions = new Actions( inputTriggerConfig );

	private final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();

	private final CrossHair crosshair = new CrossHair();

	private final Color onFocusColor = new Color( 255, 255, 255, 180 );

	private final Color outOfFocusColor = new Color( 127, 127, 127, 100 );
	{
		crosshair.setColor( outOfFocusColor );
	}

	public ViewerNode(
			final CacheControl cacheControl,
			final ViewerAxis viewerAxis,
			final ViewerOptions viewerOptions ) throws InterruptedException
	{
		this.viewer = createViewer( viewerOptions, cacheControl );
		this.setContent( this.viewer );
		this.viewerAxis = viewerAxis;
		this.state = new ViewerState( this.viewer );
		this.manager = new ViewerTransformManager( this.viewer, state, globalToViewer( viewerAxis ) );
		initializeViewer();
		addCrosshair();
	}

	private void addCrosshair()
	{

		this.focusedProperty().addListener( ( ChangeListener< Boolean > ) ( observable, oldValue, newValue ) -> {
			if ( newValue )
				crosshair.setColor( onFocusColor );
			else
				crosshair.setColor( outOfFocusColor );
			viewer.getDisplay().repaint();
		} );
	}

	public void setCrossHairColor( final int r, final int g, final int b, final int a )
	{
		this.crosshair.setColor( r, g, b, a );
	}

	public ViewerTransformManager manager()
	{
		return manager;
	}

	public void addBehaviour( final Behaviour behaviour, final String name, final String... defaultTriggers )
	{
		this.behaviours.behaviour( behaviour, name, defaultTriggers );
	}

	public void addAction( final AbstractNamedAction action, final String... defaultKeyStrokes )
	{
		this.actions.namedAction( action, defaultKeyStrokes );
	}

	public void addAction( final Runnable action, final String name, final String... defaultKeyStrokes )
	{
		this.actions.runnableAction( action, name, defaultKeyStrokes );
	}

	public void addMouseMotionListener( final MouseMotionListener listener )
	{
		this.viewer.getDisplay().addMouseMotionListener( listener );
	}

	public void removeMouseMotionListener( final MouseMotionListener listener )
	{
		this.viewer.getDisplay().removeMouseMotionListener( listener );
	}

	public void setViewerPanelState( final ViewerState state )
	{
		this.state.set( state );
	}

	private ViewerPanel createViewer( final ViewerOptions viewerOptions, final CacheControl cacheControl ) throws InterruptedException
	{
		final Object notifyObject = this;
		final ViewerPanel[] viewerStore = new ViewerPanel[ 1 ];
		SwingUtilities.invokeLater( () -> {
			viewerStore[ 0 ] = new ViewerPanel( new ArrayList<>(), 1, cacheControl, viewerOptions );
			synchronized ( notifyObject )
			{
				notifyObject.notify();
			}
		} );
		System.out.println( "Waiting for viewer to be initialized!" );
		synchronized ( notifyObject )
		{
			notifyObject.wait();
		}
		System.out.println( "Notified about initialization!" );
		return viewerStore[ 0 ];
	}

	private void initializeViewer() throws InterruptedException
	{
		final Object notifyObject = this;
		SwingUtilities.invokeLater( () -> {
			viewer.setDisplayMode( DisplayMode.FUSED );
			viewer.setMinimumSize( new Dimension( 100, 100 ) );
			viewer.setPreferredSize( new Dimension( 100, 100 ) );

			viewer.getDisplay().setTransformEventHandler( this.manager );
			this.manager.install( triggerbindings, keybindings );

			triggerbindings.addBehaviourMap( "default", behaviours.getBehaviourMap() );
			triggerbindings.addInputTriggerMap( "default", behaviours.getInputTriggerMap() );
			keybindings.addActionMap( "default", actions.getActionMap() );
			keybindings.addInputMap( "default", actions.getInputMap() );

			mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
			mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
			viewer.getDisplay().addHandler( mouseAndKeyHandler );
			viewer.getDisplay().addOverlayRenderer( crosshair );
			SwingUtilities.replaceUIActionMap( viewer.getRootPane(), keybindings.getConcatenatedActionMap() );
			SwingUtilities.replaceUIInputMap( viewer.getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
			viewer.setVisible( true );
			synchronized ( notifyObject )
			{
				notifyObject.notify();
			}
		} );
		synchronized ( notifyObject )
		{
			notifyObject.wait();
		}
	}

	public static AffineTransform3D globalToViewer( final ViewerAxis axis )
	{

		final AffineTransform3D tf = new AffineTransform3D();
		switch ( axis )
		{
		case Z:
			break;
		case Y:
			tf.rotate( 1, Math.PI / 2 );
			break;
		case X:
			tf.rotate( 0, -Math.PI / 2 );
			break;
		}
		return tf;
	}

	@Override
	public void onChanged( final javafx.collections.ListChangeListener.Change< ? extends SourceAndConverter< ? > > c )
	{
		c.next();
		if ( c.wasRemoved() )
			c.getRemoved().forEach( removed -> {
				visibility.remove( removed );
				viewer.removeSource( removed.getSpimSource() );
			} );
		else if ( c.wasAdded() )
			c.getAddedSubList().forEach( added -> {
				visibility.put( added.getSpimSource(), true );
				viewer.addSource( added );
				final int numSources = viewer.getState().numSources();
				if ( numSources > 1 )
					viewer.getVisibilityAndGrouping().setCurrentSource( 1 );
			} );
	}

	public void manageOwnLayerVisibility( final boolean manageVisibility )
	{
		this.managesOwnLayerVisibility = manageVisibility;
	}

	public boolean manageOwnLayerVisibility()
	{
		return managesOwnLayerVisibility;
	}

	public void setViewerAxis( final ViewerAxis axis )
	{
		this.viewerAxis = axis;
		this.manager.setGlobalToViewer( globalToViewer( axis ) );
	}

	public AffineTransform3D getTransformCopy()
	{
		return this.manager.getTransform();
	}

	public InputTriggerConfig inputTriggerConfig()
	{
		return inputTriggerConfig;
	}

	@Override
	public double minWidth( final double height )
	{
		return getContent() == null ? 0 : getContent().getMinimumSize().getWidth();
	}

	@Override
	public double minHeight( final double width )
	{
		return getContent() == null ? 0 : getContent().getMinimumSize().getHeight();
	}

	@Override
	public double maxWidth( final double height )
	{
		return getContent() == null ? 100 : getContent().getMaximumSize().getWidth();
	}

	@Override
	public double maxHeight( final double width )
	{
		return getContent() == null ? 100 : getContent().getMaximumSize().getHeight();
	}

	@Override
	public double prefWidth( final double height )
	{
		return getContent() == null ? 10 : getContent().getPreferredSize().getWidth();
	}

	@Override
	public double prefHeight( final double width )
	{
		return getContent() == null ? 10 : getContent().getPreferredSize().getHeight();
	}

	public ViewerState getState()
	{
		return state;
	}

}