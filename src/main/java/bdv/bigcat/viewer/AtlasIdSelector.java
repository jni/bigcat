package bdv.bigcat.viewer;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.bigcat.viewer.state.SelectedIds;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import net.imglib2.RealRandomAccess;

public class AtlasIdSelector
{

	private final HashMap< Source< ? >, RealRandomAccess< ? > > accesses = new HashMap<>();

	private final HashMap< Source< ? >, ToLongFunction > toIdConverters = new HashMap<>();

	private final HashMap< Source< ? >, SelectedIds > selectedIds;

	private final HashMap< ViewerPanel, MouseAndKeyHandler > mouseAndKeyHandlers = new HashMap<>();

	public AtlasIdSelector( final HashMap< Source< ? >, SelectedIds > selectedIds )
	{
		this.selectedIds = selectedIds;
	}

	public void addSource( final Source< ? > source, final RealRandomAccess< ? > access, final ToLongFunction< ? > toIdConverter )
	{

		this.accesses.put( source, access );
		this.toIdConverters.put( source, toIdConverter );
	}

	public Consumer< ViewerPanel > onEnter()
	{
		return t -> {
			if ( !this.mouseAndKeyHandlers.containsKey( t ) )
			{
				final InputTriggerConfig inputTriggerConfig = new InputTriggerConfig();
				final IdSelector selector = new IdSelector( t, toIdConverters, selectedIds, accesses );
				final Behaviours behaviours = new Behaviours( inputTriggerConfig );
				behaviours.namedBehaviour( selector.selectSingle( "toggle single id" ), "button1" );
				behaviours.namedBehaviour( selector.append( "append id" ), "shift button1" );
				final TriggerBehaviourBindings bindings = new TriggerBehaviourBindings();
				behaviours.install( bindings, "id selection bindings" );
				final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
				mouseAndKeyHandler.setInputMap( bindings.getConcatenatedInputTriggerMap() );
				mouseAndKeyHandler.setBehaviourMap( bindings.getConcatenatedBehaviourMap() );
				this.mouseAndKeyHandlers.put( t, mouseAndKeyHandler );
//				this.selectedIds.values().forEach( selectedIds -> selectedIds.addListener( () -> t.requestRepaint() ) );
				System.out.println( "Installed handler for " + t );// + " " +
																	// this.mouseAndKeyHandlers
																	// );
				t.getDisplay().addHandler( mouseAndKeyHandler );
			}
//			t.getDisplay().addHandler( this.mouseAndKeyHandlers.get( t ) );
		};
	}

	public Consumer< ViewerPanel > onExit()
	{
		return t -> {
//			t.getDisplay().removeHandler( this.mouseAndKeyHandlers.get( t ) );
		};
	}

}