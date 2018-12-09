package at.ac.wu.asana.viz.test;

import java.util.Collection;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class Tutorial1AlternativeConstruction {
	public static void main(String[] args) {
		
		Graph graph = new SingleGraph("Tutorial 1");
		
		graph.setStrict(false);
		graph.setAutoCreate( true );
		graph.addEdge( "AB", "A", "B" );
		graph.addEdge( "BC", "B", "C" );
		graph.addEdge( "CA", "C", "A" );
		
		Node A = graph.getNode("A");
		System.out.println(A.getDegree());
		
		A.addAttribute("Label", "A");
		A.addAttribute("ui.label", "A");
		
		Collection<Node> nodes = graph.getNodeSet();
		
		for (Node n : nodes) {
			System.out.println(n.getId()+", "+n.getAttributeKeySet()+ ", "+ n.getLabel("A"));
		}
		
		graph.display();
	}
}
