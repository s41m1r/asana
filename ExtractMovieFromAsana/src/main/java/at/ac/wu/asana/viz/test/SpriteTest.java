package at.ac.wu.asana.viz.test;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;

public class SpriteTest {

	public static void main(String[] args) {

		Graph graph = new SingleGraph("tutorial 1");
		
		SpriteManager sManager = new SpriteManager(graph);
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		graph.addAttribute("ui.stylesheet", "url('file:///home/saimir/eclipse-workspace/ExtractMovieFromAsana/src/resources/stylesheetTest.css')");
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");

		graph.display();

		graph.addEdge("AB", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CA", "C", "A");
		graph.addEdge("AD", "A", "D");
		graph.addEdge("DE", "D", "E");
		graph.addEdge("DF", "D", "F");
		graph.addEdge("EF", "E", "F");
		
		Sprite s1 = sManager.addSprite("S1");
		s1.attachToNode("A");
		s1.setPosition(0.2,0.2,0);
		Sprite s2 = sManager.addSprite("S2");
		s2.setPosition(.3);
		s2.attachToEdge("DE");

		for (Node node : graph) {
			node.addAttribute("ui.label", node.getId());
		}

	}

}
