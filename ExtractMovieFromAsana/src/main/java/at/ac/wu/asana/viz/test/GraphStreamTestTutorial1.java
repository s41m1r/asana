package at.ac.wu.asana.viz.test;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;


public class GraphStreamTestTutorial1 {
	
	public static void main(String[] args) {
		
		Graph graph = new SingleGraph("Tutorial 1");
		graph.addNode("A" );
		graph.addNode("B" );
		graph.addNode("C" );
		graph.addEdge("AB", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CA", "C", "A");
		
		int nodes = graph.getNodeCount();
		int edges = graph.getEdgeCount();
		
		System.out.println("There are "+nodes+ " nodes and "+edges+ " edges.");
	
		graph.display();
		
	}
	
}
