package bdv.bigcat;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.cache.CacheControl;
import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformListener;

public class ViewerPanalJFX
{
	public static void main( final String[] args )
	{
		Application.launch( MyApplication.class );
	}

	public static class MyApplication extends Application
	{

		public static class GridConstraintsManager
		{

			private final double defaultColumnWidth1 = 50;

			private final double defaultColumnWidth2 = 50;

			private final double defaultRowHeight1 = 50;

			private final double defaultRowHeight2 = 50;

			private final ColumnConstraints column1 = new ColumnConstraints();

			private final ColumnConstraints column2 = new ColumnConstraints();

			final RowConstraints row1 = new RowConstraints();

			final RowConstraints row2 = new RowConstraints();

			private double columnWidth1;

			private double columnWidth2;

			private double rowHeight1;

			private double rowHeight2;

			public GridConstraintsManager()
			{
				resetToDefault();
				storeCurrent();
			}

			private synchronized final void resetToDefault()
			{
				column1.setPercentWidth( defaultColumnWidth1 );
				column2.setPercentWidth( defaultColumnWidth2 );
				row1.setPercentHeight( defaultRowHeight1 );
				row2.setPercentHeight( defaultRowHeight2 );
			}

			private synchronized final void resetToLast()
			{
				column1.setPercentWidth( columnWidth1 );
				column2.setPercentWidth( columnWidth2 );
				row1.setPercentHeight( rowHeight1 );
				row2.setPercentHeight( rowHeight2 );
			}

			private synchronized void storeCurrent()
			{
				this.columnWidth1 = column1.getPercentWidth();
				this.columnWidth2 = column2.getPercentWidth();
				this.rowHeight1 = row1.getPercentHeight();
				this.rowHeight2 = row2.getPercentHeight();
			}

			private synchronized void maximize( final int r, final int c, final int steps )
			{
				storeCurrent();
				final ColumnConstraints increaseColumn = c == 0 ? column1 : column2;
				final ColumnConstraints decreaseColumn = c == 0 ? column2 : column1;
				final RowConstraints increaseRow = r == 0 ? row1 : row2;
				final RowConstraints decreaseRow = r == 0 ? row2 : row1;
				final double increaseColumnStep = ( 100 - increaseColumn.getPercentWidth() ) / steps;
				final double decreaseColumnStep = ( decreaseColumn.getPercentWidth() - 0 ) / steps;
				final double increaseRowStep = ( 100 - increaseRow.getPercentHeight() ) / steps;
				final double decreaseRowStep = ( decreaseRow.getPercentHeight() - 0 ) / steps;

				for ( int i = 0; i < steps; ++i )
				{
					increaseColumn.setPercentWidth( increaseColumn.getPercentWidth() + increaseColumnStep );
					decreaseColumn.setPercentWidth( decreaseColumn.getPercentWidth() - decreaseColumnStep );
					increaseRow.setPercentHeight( increaseRow.getPercentHeight() + increaseRowStep );
					decreaseRow.setPercentHeight( decreaseRow.getPercentHeight() - decreaseRowStep );
				}

				increaseColumn.setPercentWidth( 100 );
				decreaseColumn.setPercentWidth( 0 );
				increaseRow.setPercentHeight( 100 );
				decreaseRow.setPercentHeight( 0 );

			}

		}

		@SuppressWarnings( "unchecked" )
		@Override
		public void start( final Stage primaryStage ) throws Exception
		{
//			final StackPane root = new StackPane();
			final GridPane root = new GridPane();

			final SwingNode viewerNode1 = new SwingNode();
			final SwingNode viewerNode2 = new SwingNode();
			final SwingNode viewerNode3 = new SwingNode();
			final SwingNode[] viewerNodesArray = new SwingNode[] { viewerNode1, viewerNode2, viewerNode3 };

			final HashSet< SwingNode > viewerNodes = new HashSet<>( Arrays.asList( viewerNodesArray ) );

			final Class< ? >[] focusKeepers = { TextField.class };
			for ( int i = 0; i < viewerNodesArray.length; ++i )
			{
				final SwingNode viewerNode = viewerNodesArray[ i ];
//				final DropShadow ds = new DropShadow( 10, Color.PURPLE );
				final DropShadow ds = new DropShadow( 10, Color.hsb( 60.0 + 360.0 * i / viewerNodes.size(), 1.0, 0.5, 1.0 ) );
				viewerNode.focusedProperty().addListener( ( ChangeListener< Boolean > ) ( observable, oldValue, newValue ) -> {
					if ( newValue )
						viewerNode.setEffect( ds );
					else
						viewerNode.setEffect( null );
				} );

				viewerNode.addEventHandler( MouseEvent.MOUSE_CLICKED, event -> viewerNode.requestFocus() );

				viewerNode.addEventHandler( MouseEvent.MOUSE_ENTERED, event -> {
					final Node focusOwner = viewerNode.sceneProperty().get().focusOwnerProperty().get();
					for ( final Class< ? > focusKeeper : focusKeepers )
						if ( focusKeeper.isInstance( focusOwner ) )
							return;
					viewerNode.requestFocus();
				} );
			}

			final TableView< ? > table = new TableView<>();
			table.setEditable( true );
			table.getColumns().addAll( new TableColumn<>( "Property" ), new TableColumn<>( "Value" ) );

			final TextField tf = new TextField( "some text" );

			root.setHgap( 1 );
			root.setVgap( 1 );

			final Scene scene = new Scene( root, 500, 500 );

			final TabPane infoPane = new TabPane();

			final VBox jfxStuff = new VBox( 1 );
			jfxStuff.getChildren().addAll( tf, table );
			infoPane.getTabs().add( new Tab( "jfx stuff", jfxStuff ) );

			infoPane.getTabs().add( new Tab( "dataset info", new Label( "random floats" ) ) );

			final GridConstraintsManager gridConstraintsManager = new GridConstraintsManager();

			GridPane.setConstraints( viewerNode1, 0, 0 );
			GridPane.setConstraints( viewerNode2, 1, 0 );
			GridPane.setConstraints( viewerNode3, 0, 1 );
			GridPane.setConstraints( infoPane, 1, 1 );
			root.getChildren().add( viewerNode1 );
			root.getChildren().add( viewerNode2 );
			root.getChildren().add( viewerNode3 );
			root.getChildren().add( infoPane );

			root.getColumnConstraints().add( gridConstraintsManager.column1 );
			root.getColumnConstraints().add( gridConstraintsManager.column2 );

			root.getRowConstraints().add( gridConstraintsManager.row1 );
			root.getRowConstraints().add( gridConstraintsManager.row2 );

			final boolean[] isFullScreen = new boolean[] { false };

			primaryStage.setTitle( "BigCAT" );
			primaryStage.setScene( scene );
			createSwingContent( viewerNode1, viewerNode2, viewerNode3, root );

			scene.setOnKeyTyped( event -> {
				if ( event.getCharacter().equals( "a" ) )
				{
					final Node focusOwner = scene.focusOwnerProperty().get();
					if ( viewerNodes.contains( focusOwner ) )
					{
						event.consume();
						if ( !isFullScreen[ 0 ] )
						{
							viewerNodes.forEach( node -> node.setVisible( node == focusOwner ) );
							infoPane.setVisible( false );
							gridConstraintsManager.maximize(
									GridPane.getRowIndex( focusOwner ),
									GridPane.getColumnIndex( focusOwner ),
									0 );
							( ( ViewerPanel ) ( ( SwingNode ) focusOwner ).getContent() ).requestRepaint();
							root.setHgap( 0 );
							root.setVgap( 0 );
						}
						else
						{
							gridConstraintsManager.resetToLast();
							viewerNodes.forEach( node -> node.setVisible( true ) );
							viewerNodes.forEach( node -> ( ( ViewerPanel ) node.getContent() ).requestRepaint() );
							infoPane.setVisible( true );
							root.setHgap( 1 );
							root.setVgap( 1 );
						}
						isFullScreen[ 0 ] = !isFullScreen[ 0 ];
					}
				}
			} );

			primaryStage.show();
		}

		private void createSwingContent( final SwingNode swingNode1, final SwingNode swingNode2, final SwingNode swingNode3, final Pane root )
		{
			SwingUtilities.invokeLater( () -> {
				// this is just temporary: sleep until all javafx is set up to avoid width == 0 or height == 0 exceptions
				try
				{
					Thread.sleep( 100 );
				}
				catch ( final InterruptedException e )
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				final Random rng = new Random( 100 );
				final Img< FloatType > rai = ArrayImgs.floats( 100, 200, 300 );

				for ( final FloatType f1 : rai )
					f1.set( rng.nextFloat() );

				final Converter< FloatType, ARGBType > conv = ( s, t ) -> {
					t.set( ARGBType.rgba( 0, s.getRealDouble() * 255, 0, 0 ) );
				};

				final AffineTransform3D tf = new AffineTransform3D();

				final List< SourceAndConverter< FloatType > > sacs = BdvFunctions.toSourceAndConverter( rai, conv, AxisOrder.XYZ, tf, "ok" );
				final List< SourceAndConverter< ? > > sacsWildcard = sacs.stream().map( sac -> (SourceAndConverter< ? >)sac ).collect( Collectors.toList() );

				final GlobalTransformManager gm = new GlobalTransformManager( new AffineTransform3D() );

				gm.setTransform( new AffineTransform3D() );

				final AffineTransform3D tf1 = new AffineTransform3D();
				final AffineTransform3D tf2 = new AffineTransform3D();
				final AffineTransform3D tf3 = new AffineTransform3D();
				tf2.rotate( 1, Math.PI / 2 );
				tf3.rotate( 0, -Math.PI / 2 );

				tf2.set( tf2.copy() );
				tf3.set( tf3.copy() );

				System.out.println( "WAS IST DA LOS?" );
				System.out.println( tf2 );
				System.out.println( tf2.inverse() );

				final ViewerPanel viewer1 = makeViewer( sacsWildcard, 1, new CacheControl.Dummy(), swingNode1, root, gm, tf1 );
				final ViewerPanel viewer2 = makeViewer( sacsWildcard, 1, new CacheControl.Dummy(), swingNode2, root, gm, tf2 );
				final ViewerPanel viewer3 = makeViewer( sacsWildcard, 1, new CacheControl.Dummy(), swingNode3, root, gm, tf3 );

				viewer1.setPreferredSize( new Dimension( 200, 200 ) );
				viewer2.setPreferredSize( new Dimension( 200, 200 ) );
				viewer3.setPreferredSize( new Dimension( 200, 200 ) );

				final AffineTransform3D translation = new AffineTransform3D();
				translation.translate( rai.dimension( 0 ) / 2 * 1e-1, rai.dimension( 1 ) / 2 * 1e-1, rai.dimension( 2 ) / 2 * 1e-1 );


				final AffineTransform3D scale = new AffineTransform3D();
				scale.scale( 1e-1 );
				gm.preConcatenate( scale );

//				gm.setTransform( translation );

//				final Thread t = new Thread( () -> {
//					final AffineTransform3D rotation = new AffineTransform3D();
//					rotation.rotate( 0, 0.1 );
////					rotation.rotate( 1, 0.2 );
//					while ( !Thread.currentThread().isInterrupted() )
//					{
//						try
//						{
//							Thread.sleep( 100 );
//						}
//						catch ( final InterruptedException e )
//						{
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						gm.concatenate( rotation );
//					}
//				} );
//
//				t.start();

			} );
		}
	}

	public static ViewerPanel makeViewer(
			final List< SourceAndConverter< ? > > sacs,
			final int numTimePoints,
			final CacheControl cacheControl,
			final SwingNode swingNode,
			final Pane root,
			final GlobalTransformManager gm,
			final AffineTransform3D tf )
	{

		final ViewerPanel viewer = new ViewerPanel( sacs, numTimePoints, cacheControl );
		viewer.setMinimumSize( new Dimension( 100, 100 ) );

		final InputActionBindings keybindings = new InputActionBindings();
		final TriggerBehaviourBindings triggerbindings = new TriggerBehaviourBindings();
		final InputTriggerConfig inputTriggerConfig = new InputTriggerConfig();

		final ViewerTransformManager vtm = new ViewerTransformManager( gm, tf, viewer );
		viewer.getDisplay().setTransformEventHandler( vtm );
		vtm.install( triggerbindings );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

//		final TransformEventHandler< ? > tfHandler = viewer.getDisplay().getTransformEventHandler();
//		if ( tfHandler instanceof BehaviourTransformEventHandler )
//			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );
//
//		NavigationActions.installActionBindings( keybindings, viewer, inputTriggerConfig );

		swingNode.setContent( viewer );
		SwingUtilities.replaceUIActionMap( viewer.getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( viewer.getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		return viewer;
	}

	public static class GlobalTransformManager
	{

		private final ArrayList< TransformListener< AffineTransform3D > > listeners;

		private final AffineTransform3D affine;

		public GlobalTransformManager( final AffineTransform3D affine, final TransformListener< AffineTransform3D >... listeners )
		{
			this( affine, Arrays.asList( listeners ) );
		}

		public GlobalTransformManager( final AffineTransform3D affine, final List< TransformListener< AffineTransform3D > > listeners )
		{
			super();
			this.listeners = new ArrayList<>( listeners );
			this.affine = affine;
		}

		public synchronized void setTransform( final AffineTransform3D affine )
		{
			this.affine.set( affine );
			notifyListeners();
		}

		public void addListener( final TransformListener< AffineTransform3D > listener )
		{
			this.listeners.add( listener );
		}

		public synchronized void preConcatenate( final AffineTransform3D transform )
		{
			this.affine.preConcatenate( transform );
			notifyListeners();
		}

		public synchronized void concatenate( final AffineTransform3D transform )
		{
			this.affine.concatenate( transform );
			notifyListeners();
		}

		private synchronized void notifyListeners()
		{
			for ( final TransformListener< AffineTransform3D > l : listeners )
				l.transformChanged( this.affine );
		}

	}

	public static class ViewerTransformManager implements TransformListener< AffineTransform3D >, TransformEventHandler< AffineTransform3D >
	{

		public ViewerTransformManager(
				final GlobalTransformManager manager,
				final AffineTransform3D globalToViewer,
				final TransformListener< AffineTransform3D > listener )
		{
			super();
			this.manager = manager;
			this.globalToViewer = globalToViewer;
			this.listener = listener;
			this.manager.addListener( this );
			this.canvasH = 1;
			this.canvasW = 1;
			this.centerX = this.canvasW / 2;
			this.centerY = this.canvasH / 2;

			behaviours = new Behaviours( config, "bdv" );

			behaviours.behaviour( new TranslateXY(), "drag translate", "button2", "button3" );
		}

		private final GlobalTransformManager manager;

		private final InputTriggerConfig config = new InputTriggerConfig();

		private final AffineTransform3D global = new AffineTransform3D();

		private final AffineTransform3D concatenated = new AffineTransform3D();

		private final AffineTransform3D displayTransform = new AffineTransform3D();

		private final AffineTransform3D globalToViewer;

		private TransformListener< AffineTransform3D > listener;

		private final Behaviours behaviours;

		private int canvasW = 1, canvasH = 1;

		private int centerX = 0, centerY = 0;

		private void notifyListener()
		{
			final AffineTransform3D copy = concatenated.copy();
//			copy.preConcatenate( globalToViewer );
//			System.out.println( copy );
			listener.transformChanged( copy );
		}

		private synchronized void update()
		{
			concatenated.set( global );
			concatenated.preConcatenate( globalToViewer );
//			System.out.println( "UPDATE " + displayTransform );
			concatenated.preConcatenate( displayTransform );
			notifyListener();
		}

		@Override
		public synchronized void setTransform( final AffineTransform3D transform )
		{
			global.set( transform );
			update();
		}

		@Override
		public synchronized void transformChanged( final AffineTransform3D transform )
		{
			setTransform( transform );
		}

		@Override
		public void setCanvasSize( final int width, final int height, final boolean updateTransform )
		{
			if ( width == 0 || height == 0 )
				return;
//			System.out.println( "setCanvasSize " + width + " " + height + " " + displayTransform + " " + canvasW + " " + canvasH );
			if ( updateTransform ) // && false )
				synchronized ( this )
				{
					displayTransform.set( displayTransform.get( 0, 3 ) - canvasW / 2, 0, 3 );
					displayTransform.set( displayTransform.get( 1, 3 ) - canvasH / 2, 1, 3 );
					displayTransform.scale( ( double ) width / canvasW );
					displayTransform.set( displayTransform.get( 0, 3 ) + width / 2, 0, 3 );
					displayTransform.set( displayTransform.get( 1, 3 ) + height / 2, 1, 3 );
					update();
					notifyListener();
				}
			canvasW = width;
			canvasH = height;
			centerX = width / 2;
			centerY = height / 2;
		}

		@Override
		public synchronized AffineTransform3D getTransform()
		{
			return concatenated.copy();
		}

		@Override
		public void setTransformListener( final TransformListener< AffineTransform3D > transformListener )
		{
			this.listener = listener;
		}

		@Override
		public String getHelpString()
		{
			return "TODO";
		}

		public void install( final TriggerBehaviourBindings bindings )
		{
			behaviours.install( bindings, "transform" );
		}

		private class TranslateXY implements DragBehaviour
		{

			private int oX, oY;

			private final double[] delta = new double[ 3 ];

			private final AffineTransform3D affineDrag = new AffineTransform3D();

			@Override
			public synchronized void init( final int x, final int y )
			{
				synchronized ( global )
				{
					this.oX = x;
					this.oY = y;
					affineDrag.set( global );
				}
			}

			@Override
			public synchronized void drag( final int x, final int y )
			{
				synchronized ( global )
				{
					final double dX = ( x - oX ) / displayTransform.get( 0, 0 );
					final double dY = ( y - oY ) / displayTransform.get( 0, 0 );
					global.set( affineDrag );
					delta[ 0 ] = dX;
					delta[ 1 ] = dY;
					delta[ 2 ] = 0.0;


					globalToViewer.applyInverse( delta, delta );
					for ( int d = 0; d < delta.length; ++d )
						global.set( global.get( d, 3 ) + delta[ d ], d, 3 );
					manager.setTransform( global );
				}

			}

			@Override
			public void end( final int x, final int y )
			{}
		}

	}
}