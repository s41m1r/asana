package at.ac.wu.asana.test;

import org.graphstream.algorithm.generator.BananaTreeGenerator;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.BaseGenerator;
import org.graphstream.algorithm.generator.LobsterGenerator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.ProxyPipe;
import org.graphstream.stream.SinkAdapter;
import org.graphstream.ui.view.Viewer;

import java.util.Iterator;

/**
 * Created by pigne on 8/13/16.
 */
public class CollapseSubTree {
    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        String stylesheet = "" +
                "node {" +
                "   text-background-mode: rounded-box;" +
                "   text-background-color: gray; " +
                "   text-color: #222;" +
                "}" +
                "node.plus {" +
                "   text-background-color: #F33; " +
                "   text-color: #DDD;" +
                "}";
        final Graph g = new SingleGraph("collapse");
        g.setAttribute("stylesheet", stylesheet);

        Viewer viewer = g.display();

        //Layout l = Layouts.newLayoutAlgorithm();
        //l.setStabilizationLimit(0);
        //viewer.enableAutoLayout(l);

        BaseGenerator gen  = new LobsterGenerator();
//        gen = new BarabasiAlbertGenerator();
//        gen = new WattsStrogatzGenerator(100,2,0.5);
//        gen = new RandomEuclideanGenerator();
        gen = new BananaTreeGenerator();
        
        gen.setDirectedEdges(true, false);
        gen.addNodeLabels(true);
        gen.addSink(g);

        gen.begin();
        for (int i = 0; i < 30; i++) {
            gen.nextEvents();
        }
        gen.end();

        ProxyPipe fromViewer = viewer.newViewerPipe();
        fromViewer.addSink(g);

        fromViewer.addSink(new SinkAdapter(){
            @Override
            public void nodeAttributeAdded(String sourceId, long timeId, String nodeId, String attribute, Object value) {
                if(attribute.equals("ui.clicked")){
                    toggleNode(nodeId);
                }
            }

            @Override
            public void nodeAttributeChanged(String sourceId, long timeId, String nodeId, String attribute, Object oldValue, Object newValue) {
                if(attribute.equals("ui.clicked")){
                    toggleNode(nodeId);
                }
            }
            void toggleNode(String id){
                Node n  = g.getNode(id);
                Object[] pos = n.getAttribute("xyz");
                Iterator<Node> it = n.getBreadthFirstIterator(true);
                if(n.hasAttribute("collapsed")){
                    n.removeAttribute("collapsed");
                    while(it.hasNext()){
                        Node m  =  it.next();

                        for(Edge e : m.getLeavingEdgeSet()) {
                            e.removeAttribute("ui.hide");
                        }
                        m.removeAttribute("layout.frozen");
                        m.setAttribute("x",Double.valueOf(pos[0].toString())+Math.random()*0.0001);
                        m.setAttribute("y",Double.valueOf(pos[1].toString())+Math.random()*0.0001);

                        m.removeAttribute("ui.hide");

                    }
                    n.removeAttribute("ui.class");

                } else {
                    n.setAttribute("ui.class", "plus");
                    n.setAttribute("collapsed");

                    while(it.hasNext()){
                        Node m  =  it.next();

                        for(Edge e : m.getLeavingEdgeSet()) {
                            e.setAttribute("ui.hide");
                        }
                        if(n != m) {
                            m.setAttribute("layout.frozen");
                            m.setAttribute("x", Double.valueOf(pos[0].toString()) + Math.random() * 0.0001);
                            m.setAttribute("y", Double.valueOf(pos[1].toString()) + Math.random() * 0.0001);

                            m.setAttribute("ui.hide");
                        }

                    }
                }
            }
        });

        while(true){
            try {
                fromViewer.blockingPump();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
