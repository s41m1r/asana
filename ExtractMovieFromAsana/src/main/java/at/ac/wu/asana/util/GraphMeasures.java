package at.ac.wu.asana.util;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.algorithm.community.EpidemicCommunityAlgorithm;
import org.graphstream.algorithm.measure.CommunityDistribution;
import org.graphstream.algorithm.measure.ConnectivityMeasure.EdgeConnectivityMeasure;
import org.graphstream.algorithm.measure.ConnectivityMeasure.VertexConnectivityMeasure;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public abstract class GraphMeasures {
	
	public static double computeAvgDegree(Graph graph) {
		double avg = 0.0;
		for (Node n : graph) {
			avg+=n.getDegree();
		}
		avg = avg/graph.getNodeCount();
		return avg;
	}
	
	public static EpidemicCommunityAlgorithm computeEpicCommunity(Graph graph) {
		EpidemicCommunityAlgorithm eca = new EpidemicCommunityAlgorithm(graph);
		eca.compute();
		return eca;
	}
	
	
	public static CommunityDistribution computeCommunityDistribution() {
		CommunityDistribution cd = new CommunityDistribution("ui.class");
		cd.compute();
		return cd;
	}
	
	public static BetweennessCentrality computeBetweennessCentrality(Graph graph) {

		System.out.println("Betwenness Centrality:");
		BetweennessCentrality bcb = new BetweennessCentrality();
		bcb.setWeightAttributeName("weight");
		bcb.init(graph);
		bcb.compute();
		
		return bcb;
	}
	
	public static int computeVertexConnectivity(Graph graph) {
		VertexConnectivityMeasure vcm = new VertexConnectivityMeasure();
		vcm.init(graph);
		vcm.compute();
		return vcm.getVertexConnectivity();
	}
	
	public static int computeEdgeConnectivity(Graph graph) {
		EdgeConnectivityMeasure vcm = new EdgeConnectivityMeasure();
		vcm.init(graph);
		vcm.compute();
		return vcm.getEdgeConnectivity();
	}
	
	public static void computeCliques(Graph graph) {
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
}
