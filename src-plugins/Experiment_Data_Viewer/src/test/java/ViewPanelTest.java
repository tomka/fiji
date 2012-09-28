import java.awt.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;

import org.junit.Assert;
import org.junit.Test;

import model.View;
import ui.ViewPanel;

public class ViewPanelTest {

	/**
	 * A testing implementation of the ViewPanel class.
	 */
	class TestViewPanel extends ViewPanel<Object> {
		public TestViewPanel(View view, String title) {
			super( view, title );
		}

		@Override
		public Object loadData( String path ) {
			return new Object();
		}

		@Override
		public List<Component> getContent( Object data ) {
			List<Component> components = new ArrayList();
			components.add( new JLabel() );
			return components;
		}

		@Override
		public String getTabText( int counter, int subcomponent ) {
			return "";
		}
	}

	@Test
	public void construction() {
		String name = "TestName";
		List<String> paths = new ArrayList<String>();
		Map<String,Object> settings = new HashMap<String,Object>();
		View v = new View( name, paths, settings);

		String title = "TestTitle";
		TestViewPanel vp = new TestViewPanel( v, title );

		Assert.assertEquals( title, vp.getTitle() );
	}
}
