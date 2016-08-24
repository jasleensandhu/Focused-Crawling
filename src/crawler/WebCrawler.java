package crawler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawler {
	static final String domainName = "https://cse.uta.edu";
	static final String hrefTag = "a[href]";
	static final String href = "href";
	static Set<String> finalLinkSet;
	static Set<String> nextLinkSet;
	static Set<String> visitedLinks = new HashSet<String>();
	static List<String> pendingLinks = new LinkedList<String>();
	static Map<String, String> outVertexes = new HashMap<String, String>();
	static Map<String, String> inVertexes = new HashMap<String, String>();
	static DirectedGraph<String, DefaultEdge> directedWebGraph = new DefaultDirectedGraph<String, DefaultEdge>(
			DefaultEdge.class);

	/*
	 * retrieve the links from the page parameter : String url: the page that
	 * has to be crawled
	 */
	static Set<String> getPageLinks(String url) {
		Set<String> linkSet = new HashSet<String>();
		String absoluteUrl = "";
		try {
			Document doc = Jsoup.connect(url).ignoreContentType(true).get();
			Elements linksOnPage = doc.select(hrefTag);
			if (!linksOnPage.isEmpty()) {
				for (Element link : linksOnPage) {
					absoluteUrl = link.absUrl(href);
					if (!absoluteUrl.contains("#") && absoluteUrl.contains(domainName)) {
						linkSet.add(absoluteUrl);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("page not available");
		}
		return linkSet;
	}

	/*
	 * link the node with the child nodes to form the graph and compute the in
	 * vertex and out vertexes for nodes parameter : String root : parent node,
	 * Set linkSet : links to be linked with the node
	 */
	public static void joinNodes(String root, Set<String> linkSet) {
		String outVertexValue = "";
		String inVertexValue = "";
		if (!directedWebGraph.containsVertex(root)) {
			directedWebGraph.addVertex(root);
		}
		if (!linkSet.isEmpty()) {
			for (String link : linkSet) {
				directedWebGraph.addVertex(link);
				directedWebGraph.addEdge(root, link);
				outVertexValue = link;
				if (outVertexes.containsKey(root)) {
					outVertexValue = outVertexes.get(root);
					outVertexValue = outVertexValue + "," + link;
				}
				outVertexes.put(root, outVertexValue);
				inVertexValue = root;
				if (inVertexes.containsKey(link)) {
					inVertexValue = inVertexes.get(link);
					inVertexValue = inVertexValue + "," + root;
				}
				inVertexes.put(link, inVertexValue);
			}
		}
	}

	/*
	 * Computes the values in the distance matrix
	 */
	public static int[][] computeDistanceMatrix(Map<Integer, String> vertexValueMap) {
		int[][] distanceMatrix = new int[vertexValueMap.size() + 1][vertexValueMap.size() + 1];
		DefaultEdge edge;
		for (int i = 0; i < vertexValueMap.size() + 1; i++) {
			for (int j = 0; j < vertexValueMap.size() + 1; j++) {
				edge = directedWebGraph.getEdge(vertexValueMap.get(i), vertexValueMap.get(j));
				if (edge != null) {
					distanceMatrix[i][j] = 1;
				} else {
					distanceMatrix[i][j] = 0;
				}
			}
		}
		return distanceMatrix;
	}

	/*
	 * Calculates the diameter of the graph provided
	 */
	public static void computeGraphDiameter(int[][] distanceMatrix, Map<Integer, String> vertexMap) {
		int maxValue = 0;
		String startVertex = "";
		String lastVertex = "";
		for (int k = 0; k < distanceMatrix.length; k++) {
			for (int i = 0; i < distanceMatrix.length; i++) {
				for (int j = 0; j < distanceMatrix.length; j++) {
					if (distanceMatrix[i][j] > distanceMatrix[i][k] - distanceMatrix[k][j]) {
						distanceMatrix[i][j] = distanceMatrix[i][k] + distanceMatrix[k][j];
					}
				}
			}
		}
		for (int m = 0; m < distanceMatrix.length; m++) {
			for (int n = 0; n < distanceMatrix.length; n++) {
				if (distanceMatrix[m][n] > maxValue) {
					maxValue = distanceMatrix[m][n];
					startVertex = vertexMap.get(m);
					lastVertex = vertexMap.get(n);
				}
			}
		}
		System.out.println("the start vertex is : " + startVertex);
		System.out.println("the last vertex is : " + lastVertex);
		System.out.println("the diameter is :" + maxValue);
	}

	/*
	 * Maps the vertex values to integers for matrix creations
	 */
	public static Map<Integer, String> assignVertexValues() {
		int i = 0;
		Map<Integer, String> vertexValueMap = new HashMap<Integer, String>();
		Set<String> vertexSet = directedWebGraph.vertexSet();
		for (String vertexValue : vertexSet) {
			vertexValueMap.put(i++, vertexValue);
		}
		return vertexValueMap;
	}

	public static void main(String args[]) throws FileNotFoundException {
		PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
		System.setOut(out);
		String linkValue;
		int[][] distanceMatrix;
		pendingLinks.add(domainName);
		while (!pendingLinks.isEmpty()) {
			linkValue = pendingLinks.remove(0);
			if (!visitedLinks.contains(linkValue)) {
				nextLinkSet = getPageLinks(linkValue);
				joinNodes(linkValue, nextLinkSet);
				for (String link : nextLinkSet) {
					pendingLinks.add(link);
				}
				visitedLinks.add(linkValue);
			}
		}
		Map<Integer, String> vertexMap = assignVertexValues();
		distanceMatrix = computeDistanceMatrix(vertexMap);
		computeGraphDiameter(distanceMatrix, vertexMap);
		out.println("the inbound vertexes are :");
		for (Map.Entry<String, String> entry : inVertexes.entrySet()) {
			out.println("indegree ::" + directedWebGraph.inDegreeOf(entry.getKey()));
			out.println("key ::" + entry.getKey());
			out.println("value ::" + entry.getValue());
		}
		out.println("the outbound vertexes are :");
		for (Map.Entry<String, String> entry : outVertexes.entrySet()) {
			out.println("outdegree ::" + directedWebGraph.outDegreeOf(entry.getKey()));
			out.println("key ::" + entry.getKey());
			out.println("value ::" + entry.getValue());
		}
	}
}
