package modules.tree_building.suffixTreeClustering.st_interface;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import modules.tree_building.suffixTreeClustering.data.Node;
import modules.tree_building.suffixTreeClustering.data.Type;

/**
 * Main data structure to work with in clustering. Is translated from XML input.
 * 
 * @author neumannm
 */
public class SuffixTreeInfo {

	/* type = "document" */
	private int numberOfTypes;
	/* number of nodes of the tree - does not contain root or terminal nodes */
	private int numberOfNodes;

	private Set<Type> types;
	private Node[] nodes;

	/**
	 * Constructor.
	 */
	public SuffixTreeInfo() {
		this.types = new TreeSet<Type>();
		this.nodes = new Node[]{};
	}

	/**
	 * Add a node.
	 * 
	 * @param node
	 *            - Node to add.
	 */
	public void addNode(Node node) {
		this.nodes[node.getNodeNumber() - 2] = node;
	}

	/**
	 * Set how many Types we put into the Suffix Tree.
	 * 
	 * @param number
	 *            - Number of types.
	 */
	public void setNumberOfTypes(int number) {
		this.numberOfTypes = number;
	}

	/**
	 * Set how many Nodes the suffix Tree contains (not counting root node).
	 * 
	 * @param number
	 *            - Number of nodes.
	 */
	public void setNumberOfNodes(int number) {
		this.numberOfNodes = number;
		this.nodes = new Node[this.numberOfNodes];
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public int getNumberOfTypes() {
		return numberOfTypes;
	}

	public List<Node> getNodes() {
		return Arrays.asList(nodes);
	}

	public Node getNodeByNumber(int i) {
		if (i < 1)
			return null;
		return nodes[i - 1];
	}

	public Set<Type> getTypes() {
		return types;
	}

	public void setTypes(Set<Type> types) {
		this.types = types;
	}

	public void addType(Type newType) {
		this.types.add(newType);
	}
}