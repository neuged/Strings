package modules.clustering.suffixTreeClusteringModuleWrapper;

//java standard imports:
import java.util.Properties;
import java.util.List;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

//modularization imports:
import modules.Pipe;
import modules.tree_building.suffixTreeClustering.clustering.flat.FlatCluster;
import modules.tree_building.suffixTreeClustering.clustering.flat.FlatClusterer;
import modules.tree_building.suffixTreeClustering.clustering.hierarchical.HierarchicalCluster;
import modules.tree_building.suffixTreeClustering.clustering.hierarchical.HierarchicalClusterer;
import modules.tree_building.suffixTreeClustering.clustering.neighborjoin.NeighborJoining;
import modules.tree_building.suffixTreeClustering.data.Node;
import modules.tree_building.suffixTreeClustering.data.Type;
import modules.tree_building.suffixTreeClustering.st_interface.SuffixTreeInfo;
import modules.CharPipe;
import modules.BytePipe;
import modules.InputPort;
import modules.ModuleImpl;
import modules.OutputPort;
import common.parallelization.CallbackReceiver;
import modules.vectorization.suffixTreeVectorizationWrapper.SuffixTreeInfoSer;

//google gson imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * This is a wrapper to modularize the vectorization process. 
 * @author Christopher Kraus
 * parts of this code are refactored from neumannm
 */

public class SuffixTreeClusteringWrapperV2 extends ModuleImpl {
	
	// property keys:
	
	// this property saves the selected form of clustering
	public static final String PROPERTYKEY_CLUST = "clustering type";
	
	// this property saves the name of the used corpus
	public static final String PROPERTYKEY_CORPNAME = "corpus/text name";
	
	// variables:
	
	// variable for saving the corpus 
	private SuffixTreeInfo corpus;
	
	// serializable SuffixTreeInfo variable 
	private SuffixTreeInfoSer corpusSer;
	
	// variable which holds the types
	private List<Type> types;
	
	// the type of clustering which should be applied
	private String clusterType;
	
	// the name of the corpus
	private String corpusName;
	
	//the result of the clustering
	private String clustResult;
	
	// flat cluster kmeans result
	List<FlatCluster> kmeansRes;
	
	// definitions of I/O variables
	private final String INPUTID = "byteInput";
	private final String OUTPUTID = "output";
	private final String OUTPUTJSONID = "json";
	
	// end variables
	
	// constructors:
	
	public SuffixTreeClusteringWrapperV2 (CallbackReceiver callbackReceiver,
	Properties properties) throws Exception {
	
	// call parent constructor
	super(callbackReceiver, properties);
	
	// module description
	this.setDescription("This is a wrapper to modularize the clustering process.<br/>"
		+ "It takes one input:<br/>"
		+ "<ul type =\"disc\" ><li>vectors of the type \"SuffixTreeInfo\" in JSON format</li></ul>");
	
	// property descriptions 
	this.getPropertyDescriptions().put(PROPERTYKEY_CLUST, "Three possible clustering types: \"NJ\" "
		+ "(Neighbor Joining), \"KM\" (flat k-means), \"HAC\" (hierachial agglomerative clustering)");
	
	this.getPropertyDescriptions().put(PROPERTYKEY_CORPNAME, "Insert corpus/text name");
	
	// property defaults
	this.getPropertyDefaultValues().put(ModuleImpl.PROPERTYKEY_NAME, "SuffixTreeClusteringWrapperV2"); 
	this.getPropertyDefaultValues().put(PROPERTYKEY_CLUST, "KM");
	this.getPropertyDefaultValues().put(PROPERTYKEY_CORPNAME, "myCorpus");
	
	// I/O definition
	InputPort inputPortVec = new InputPort(INPUTID, "[byte] deserialized vector after \"SuffixTreeInfoSer\".", this);
	inputPortVec.addSupportedPipe(BytePipe.class);
	
	OutputPort outputPort = new OutputPort(OUTPUTID, "[text] Plain text character output.", this);
	outputPort.addSupportedPipe(CharPipe.class);
	
	OutputPort outputJsonPort = new OutputPort(OUTPUTJSONID, "[JSON] text character output.", this);
	outputJsonPort.addSupportedPipe(CharPipe.class);
	
	// add I/O ports to instance
	super.addInputPort(inputPortVec);
	
	super.addOutputPort(outputPort);
	super.addOutputPort(outputJsonPort);
	}
	
	// process()
	@Override
	public boolean process() throws Exception {
				
		// byte input
		BytePipe pipe = (BytePipe) this.getInputPorts().get(INPUTID).getPipe();
		ObjectInputStream oiStream = new ObjectInputStream(pipe.getInput());
		this.corpusSer = (SuffixTreeInfoSer) oiStream.readObject();
		oiStream.close();
		
		// Prepare proper SuffixTreeInfo object "corpus" with all vectors.
		this.corpus = new SuffixTreeInfo();
		this.corpus.setNumberOfNodes(this.corpusSer.getNumberOfNodes());
		this.corpus.setNumberOfTypes(this.corpusSer.getNumberOfTypes());
		this.corpus.setTypes(this.corpusSer.getTypes());
		for (Node i : this.corpusSer.getNodes()) {
			this.corpus.addNode(i);
		}

		types = new ArrayList<Type>(this.corpus.getTypes());
		
		// selection of the clustering type
		switch (clusterType) {
		case "NJ":
			clusterNeighborJoin(types);
			break;
		case "KM":
			clusterFlat(types, corpusName);
			break;
		case "HAC":
			clusterHierarchical(types, corpusName);
			break;
		default:
			clusterFlat(types, corpusName);
			break;
		}
		
		// put the results into the output char pipe
		this.getOutputPorts().get(OUTPUTID).outputToAllCharPipes(this.clustResult);
		
		Iterator <Pipe> jsonPipes = this.getOutputPorts().get(OUTPUTJSONID).getPipes(CharPipe.class).iterator();
		
		while(jsonPipes.hasNext()) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(this.kmeansRes, ((CharPipe)jsonPipes.next()).getOutput());
		}
		
		// put the results of each clustering into an appropriate JSON format. But this is not that easy to do, as there are 3 different
		// output formats: FlatCluster, HierachialCluster, NeighborJoining. How to combine the output formats to one?
		
		// close outputs
		this.closeAllOutputs();
		
		// success
		return true;
	}
	
	// applyProperties()
	@Override
	public void applyProperties() throws Exception {
		
		// Set defaults for properties not yet set
		super.setDefaultsIfMissing();
		
		// Apply own properties
		this.corpusName = this.getProperties().getProperty(PROPERTYKEY_CORPNAME, this.getPropertyDefaultValues().get(PROPERTYKEY_CORPNAME));
		this.clusterType = this.getProperties().getProperty(PROPERTYKEY_CLUST, this.getPropertyDefaultValues().get(PROPERTYKEY_CLUST));
		
		// Apply parent object's properties (just the name variable actually)
		super.applyProperties();
	}
	
	// methods:
	
	private void clusterFlat(List<Type> types, String name) {
		FlatClusterer f_analysis = new FlatClusterer(types);
		
		List<FlatCluster> fClusters = f_analysis.analyse(3, 10);
		this.kmeansRes = fClusters;
		for (FlatCluster cluster : fClusters) {
			System.out.println(cluster.getMedoid().getString());
		}
		this.clustResult = f_analysis.toDot();
		
	}

	private void clusterHierarchical(List<Type> types,
			String name) {
		// usage of hierachial clustering 
		HierarchicalClusterer h_analysis = new HierarchicalClusterer(types);
		//LOGGER.info("Hierarchisches Clustern von " + types.size() + " Types");
		h_analysis.analyze();
		List<HierarchicalCluster> hClusters = h_analysis.getClusters();

		if (hClusters.size() == 1) {
			this.clustResult = h_analysis.toDot();
			
		} else
			System.err.println("Hierarchical Clustering didn't come up with exactly 1 cluster!");
	}
	
	private void clusterNeighborJoin(List<Type> types) {
		// usage of Neighbor Joining 
		NeighborJoining nj = new NeighborJoining(types);
		//LOGGER.info("Neighbor Joining Clustern von " + types.size() + " Types");
		nj.start();
		this.clustResult = nj.getTree();
	}
	
	// end methods
}
