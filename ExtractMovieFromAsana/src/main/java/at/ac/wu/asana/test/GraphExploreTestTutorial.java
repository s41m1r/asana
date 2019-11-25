package at.ac.wu.asana.test;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.graphstream.algorithm.measure.DegreeCentrality;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;

public class GraphExploreTestTutorial {

	protected String styleSheet =
	        "node {" +
	        "	fill-color: black;" +
	        "   shape: rounded-box;" +
	        "}" +
	        "node.marked {" +
	        "	fill-color: red;" +
	        "}" + 
	        "edge.marked {" +
	        "	fill-color: yellow;" +
	        "	size: 2px;" +
	        "}" 
	        ;


	public static void main(String[] args) throws IOException {
		new GraphExploreTestTutorial();
	}
	
	public GraphExploreTestTutorial() throws IOException {
		Graph graph = new SingleGraph("tutorial 1");
		Layout layout = new SpringBox(false);
	    graph.addSink(layout);
	    layout.addAttributeSink(graph);
	    
	    
		
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
        String outFile = "/home/saimir/Downloads/butta/screenshot";
        
        for (Node node : graph) {
			node.addAttribute("ui.label", node.getId());
		}
        
        Date d = new Date();
        graph.addAttribute("ui.screenshot", outFile+d.toInstant()+".png");
        explore(graph.getNode("A"), graph, outFile);
        
        FileSinkDGS dgs = new FileSinkDGS();
		dgs.writeAll(graph, "/home/saimir/Downloads/butta/dgs");
  
	}
	
	public void explore(Node source, Graph graph, String outFile ) {
        Iterator<? extends Node> k = source.getBreadthFirstIterator();
        Date d=new Date();
		graph.addAttribute("ui.screenshot", d.toInstant()+outFile);
        while (k.hasNext()) {
            Node next = k.next();
            next.setAttribute("ui.class", "marked");
            for (Edge edge : next.getEdgeSet()) {
            	String classA = edge.getNode0().getAttribute("ui.class");
            	String classB = edge.getNode1().getAttribute("ui.class");
				if(classA!=null && classB !=null && classA.equals("marked") && 
						classB.equals("marked"))
					edge.setAttribute("ui.class", "marked");
			}
            d=new Date();
    		graph.addAttribute("ui.screenshot", outFile+d.toInstant()+".png");
            sleep();
        }
	}
	
	protected void sleep() {
		try { Thread.sleep(300); } catch (Exception e) {}
	}

}
