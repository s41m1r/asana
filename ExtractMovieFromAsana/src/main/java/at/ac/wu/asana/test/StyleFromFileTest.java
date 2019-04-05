package at.ac.wu.asana.test;

import java.util.Iterator;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class StyleFromFileTest {
	
	public static void main(String[] args) {
		new StyleFromFileTest();
	}
	
	public StyleFromFileTest() {
		
		Graph graph = new SingleGraph("tutorial 1");
		
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		
		graph.addAttribute("ui.stylesheet", "url('file:///home/saimir/eclipse-workspace/ExtractMovieFromAsana/src/resources/stylesheetTest.css')");
        graph.setStrict(false);
        graph.setAutoCreate(true);
        
        graph.display();
        
        graph.addEdge("AB", "A", "B");
        graph.addEdge("BC", "B", "C");
        graph.addEdge("CA", "C", "A");
        graph.addEdge("AD", "A", "D");
        graph.addEdge("DE", "D", "E");
        graph.addEdge("DF", "D", "F");
        graph.addEdge("EF", "E", "F");
        
        for (Node node : graph) {
			node.addAttribute("ui.label", node.getId());
		}
        
        explore(graph.getNode("A"));
	}
	
	public void explore(Node source) {
        Iterator<? extends Node> k = source.getBreadthFirstIterator();

        while (k.hasNext()) {
            Node next = k.next();
            next.setAttribute("ui.class", "marked");
            sleep();   
        }
	}
	
	protected void sleep() {
		try { Thread.sleep(1000); } catch (Exception e) {}
	}
}
