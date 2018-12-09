package at.ac.wu.asana.viz.test;

import java.util.Iterator;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class GraphExploreTestTutorial {

	protected String styleSheet =
	        "node {" +
	        "	fill-color: black;" +
	        "   shape: rounded-box;" +
	        "}" +
	        "node.marked {" +
	        "	fill-color: red;" +
	        "}";


	public static void main(String[] args) {
		new GraphExploreTestTutorial();
	}
	
	public GraphExploreTestTutorial() {
		Graph graph = new SingleGraph("tutorial 1");
		
	    graph.addAttribute("ui.stylesheet", styleSheet);
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
