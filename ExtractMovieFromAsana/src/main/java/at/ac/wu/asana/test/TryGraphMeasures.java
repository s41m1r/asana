package at.ac.wu.asana.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.algorithm.ConnectedComponents;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.community.EpidemicCommunityAlgorithm;
import org.graphstream.algorithm.measure.ClosenessCentrality;
import org.graphstream.algorithm.measure.CommunityDistribution;
import org.graphstream.algorithm.measure.ConnectivityMeasure;
import org.graphstream.algorithm.measure.ConnectivityMeasure.EdgeConnectivityMeasure;
import org.graphstream.algorithm.measure.ConnectivityMeasure.VertexConnectivityMeasure;
import org.graphstream.algorithm.measure.EigenvectorCentrality;
import org.graphstream.algorithm.measure.Modularity;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class TryGraphMeasures {
	public static void main(String[] args) {
		Graph graph = new SingleGraph("Graph measures");

		/*
		 *  
		 *  	//    E----D  AB=1, BC=5, CD=3, DE=2, BE=6, EA=4  
                //   /|    |  Cb(A)=4
                //  / |    |  Cb(B)=2
                // A  |    |  Cb(C)=0
                //  \ |    |  Cb(D)=2
                //   \|    |  Cb(E)=4
                //    B----C
		 */

		graph.setStrict(false);
		graph.setAutoCreate(true);

		graph.addEdge("AC", "A", "C");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CD", "C", "D");
		graph.addEdge("CE", "C", "E");
		graph.addEdge("DE", "D", "E");
		graph.addEdge("DF", "D", "F");

		graph.addEdge("FG", "F", "G");
		graph.addEdge("GE", "G", "E");
		//		graph.addEdge("BC", "B", "C");
		graph.addEdge("EF", "E", "F");
		graph.addEdge("GD", "G", "D");
		//		graph.addEdge("AB", "A", "B");

		for (Node node : graph) {
			node.addAttribute("ui.label", node.getId());
		}

		graph.display();

		computeBetweennessCentrality(graph);
		computeClosenessCentrality(graph);	
		computeEigenVectorCentrality(graph);

		computeConnectivityMeasure(graph);

		computeConnectedComponents(graph);

		//		computeEpicCommunity(graph);

		computeAvgDegree(graph);

		computeVertexConnectivity(graph);

		computeEdgeConnectivity(graph);

		computeCommunityDistribution(graph);

		computeModularity(graph);

		computeCliques(graph);

		computeGraphMeasures(graph);


	}


	private static void computeGraphMeasures(Graph graph) {

		System.out.println("More Measures:");

		System.out.print("averageClusteringCoefficient: "+
				Toolkit.averageClusteringCoefficient(graph));
		System.out.println();

		System.out.println("averageDegree: "+Toolkit.averageDegree(graph));
		System.out.println();
		
		System.out.println("degreeAverageDeviation: "+
				Toolkit.degreeAverageDeviation(graph));
		System.out.println();
		
		System.out.println("degreeMap: "+
				Toolkit.degreeMap(graph));
		System.out.println();
		
		int[] dd = Toolkit.degreeDistribution(graph);
		System.out.println("degreeDistribution: "+ Arrays.toString(dd));

		System.out.println("clusteringCoefficients: "+
				Arrays.toString(Toolkit.clusteringCoefficients(graph)));
		System.out.println();

		System.out.println("density: "+
				Toolkit.density(graph));
		System.out.println();

		System.out.println("diameter: "+
				Toolkit.diameter(graph));
		System.out.println();	

	}


	private static void computeEigenVectorCentrality(Graph graph) {
		System.out.println("Eigenvector centrality:");
		EigenvectorCentrality ec = new EigenvectorCentrality();
		ec.init(graph);
		ec.compute();
		System.out.println("A="+ graph.getNode("A").getAttribute(ec.getCentralityAttribute()));
		System.out.println("B="+ graph.getNode("B").getAttribute(ec.getCentralityAttribute()));
		System.out.println("C="+ graph.getNode("C").getAttribute(ec.getCentralityAttribute()));
		System.out.println("D="+ graph.getNode("D").getAttribute(ec.getCentralityAttribute()));
		System.out.println("E="+ graph.getNode("E").getAttribute(ec.getCentralityAttribute()));
		System.out.println("F="+ graph.getNode("F").getAttribute(ec.getCentralityAttribute()));
		System.out.println("G="+ graph.getNode("G").getAttribute(ec.getCentralityAttribute()));
	}


	private static void computeClosenessCentrality(Graph graph) {
		System.out.println("Closeness centrality:");
		ClosenessCentrality cc = new ClosenessCentrality();
		cc.init(graph);
		cc.compute();
		System.out.println("A="+ graph.getNode("A").getAttribute("closeness"));
		System.out.println("B="+ graph.getNode("B").getAttribute("closeness"));
		System.out.println("C="+ graph.getNode("C").getAttribute("closeness"));
		System.out.println("D="+ graph.getNode("D").getAttribute("closeness"));
		System.out.println("E="+ graph.getNode("E").getAttribute("closeness"));
		System.out.println("F="+ graph.getNode("F").getAttribute("closeness"));
		System.out.println("G="+ graph.getNode("G").getAttribute("closeness"));
	}


	private static void computeCliques(Graph graph) {
		System.out.println("Cliques: ");
		List<Node> maximumClique = new ArrayList<Node>();
		int i = 0;
		for (List<Node> clique : Toolkit.getMaximalCliques(graph)) {
			System.out.println("Clique "+(i++)+": "+clique);
			if (clique.size() > maximumClique.size())
				maximumClique = clique;
		}
		System.out.println("Max clique: "+maximumClique);
	}


	private static void computeConnectivityMeasure(Graph graph) {
		System.out.println("Connectivity Measures:");
		System.out.println("Edge connectivity="+ConnectivityMeasure.getEdgeConnectivity(graph)
		+" Vertex connectivity="+ConnectivityMeasure.getVertexConnectivity(graph));
		Node[] nodes = ConnectivityMeasure.getKDisconnectingNodeTuple(graph, ConnectivityMeasure.getVertexConnectivity(graph));
		System.out.println("1-Disconnectig node tuple:");
		for (Node node : nodes) {
			System.out.println(node);
		}
		Edge[] edges = ConnectivityMeasure.getKDisconnectingEdgeTuple(graph, ConnectivityMeasure.getEdgeConnectivity(graph));
		System.out.println("1-Disconnectig edge tuple:");
		for (Edge e : edges) {
			System.out.println(e);
		}
	}


	private static void computeModularity(Graph graph) {
		Modularity m = new Modularity();
		m.init(graph);
		m.compute();
		System.out.println("Modularity: ");
		System.out.println(m.getMeasure());
	}


	private static void computeCommunityDistribution(Graph graph) {
		CommunityDistribution cd = new CommunityDistribution("cd");
		cd.init(graph);
		cd.compute();
		System.out.println("Community Distribution");
		System.out.println(cd.getMeasure()+ " "+cd.minCommunitySize() + " "+ cd.average());
	}


	private static void computeEdgeConnectivity(Graph graph) {
		EdgeConnectivityMeasure vcm = new EdgeConnectivityMeasure();
		vcm.init(graph);
		vcm.compute();
		System.out.println("Edge connectivity");
		System.out.println(vcm.getEdgeConnectivity());
	}


	private static void computeVertexConnectivity(Graph graph) {
		VertexConnectivityMeasure vcm = new VertexConnectivityMeasure();
		vcm.init(graph);
		vcm.compute();
		System.out.println("Vertex connectivity");
		System.out.println(vcm.getVertexConnectivity());
	}

	private static void computeAvgDegree(Graph graph) {
		double avg = 0.0;
		for (Node n : graph) {
			avg+=n.getDegree();
		}
		avg = avg/graph.getNodeCount();
		System.out.println("Average node degree:\n"+avg);
	}

	private static void computeEpicCommunity(Graph graph) {
		EpidemicCommunityAlgorithm eca = new EpidemicCommunityAlgorithm(graph);
		eca.compute();
		System.out.println(eca);
	}

	private static void computeConnectedComponents(Graph graph) {
		ConnectedComponents cc = new ConnectedComponents();
		cc.init(graph);
		System.out.println("Connected Components:");
		System.out.println(cc.getConnectedComponentsCount());
	}

	private static void computeBetweennessCentrality(Graph graph) {

		System.out.println("Betwenness Centrality:");
		BetweennessCentrality bcb = new BetweennessCentrality();
		bcb.setWeightAttributeName("weight");
		//		bcb.setWeight(graph.getNode("A"), graph.getNode("B"), 1);
		//		bcb.setWeight(graph.getNode("B"), graph.getNode("C"), 6);
		//		bcb.setWeight(graph.getNode("B"), graph.getNode("C"), 5);
		//		bcb.setWeight(graph.getNode("E"), graph.getNode("D"), 2);
		//		bcb.setWeight(graph.getNode("C"), graph.getNode("D"), 3);
		//		bcb.setWeight(graph.getNode("A"), graph.getNode("E"), 4);

		bcb.init(graph);
		bcb.compute();

		System.out.println("A="+ graph.getNode("A").getAttribute("Cb"));
		System.out.println("B="+ graph.getNode("B").getAttribute("Cb"));
		System.out.println("C="+ graph.getNode("C").getAttribute("Cb"));
		System.out.println("D="+ graph.getNode("D").getAttribute("Cb"));
		System.out.println("E="+ graph.getNode("E").getAttribute("Cb"));
		System.out.println("F="+ graph.getNode("F").getAttribute("Cb"));
		System.out.println("G="+ graph.getNode("G").getAttribute("Cb"));
	}
}
