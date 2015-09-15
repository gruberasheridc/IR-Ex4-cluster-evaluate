package ir.websearch.clustering.evaluate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;

public class EvaluateClustering {

	private static final String BUSINESS = "business";
	private static final String ENTERTAINMENT = "entertainment";
	private static final String POLITICS = "politics";
	private static final String SPORT = "sport";
	private static final String TECH = "tech";
	private static final String NEW_LINE = System.lineSeparator();

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Must accept 3 parameter: (1) Gold standard root dir (2) Clustering output file. (3) Report output file.");
			return;
		}

		// Read the gold standard files and create a map from DocID to ClusterID (Where DocID is a string: <original cluster ID><file number>).
		String gsRootDir = args[0];
		Map<String, String> gsDocIDToClusterID = createDocIDToClusterIDMap(gsRootDir); //createQueryDocsTruthMap(truthFile);
		if (gsDocIDToClusterID == null) {
			System.out.println("Faild to process the gold standard files. Root dir: " + gsRootDir + ".");
		}

		// Read the clustering algorithm output file and create a map from ClusterNumber to DocIDs (Where DocID is a string: <original cluster ID><file number>).		
		String clusteringOutputFile = args[1];
		Map<Integer, List<String>> clusterNumToDocIDs = createClusterNumToDocIDsMap(clusteringOutputFile);		
		if (clusterNumToDocIDs == null) {
			System.out.println("Faild to process the clustering algorithm output file: " + clusteringOutputFile + ".");
			return;
		}

		// Calculate the purity of each cluster.
		clusterNumToDocIDs.entrySet().forEach(entry -> {
			Integer clusterNum = entry.getKey();
			MutablePair<String, Integer> businessCount = new MutablePair<>(BUSINESS, 0);
			MutablePair<String, Integer> entertainmentCount = new MutablePair<>(ENTERTAINMENT, 0);
			MutablePair<String, Integer> politicsCount = new MutablePair<>(POLITICS, 0);
			MutablePair<String, Integer> sportCount = new MutablePair<>(SPORT, 0);
			MutablePair<String, Integer> techCount = new MutablePair<>(TECH, 0);

			List<String> docIDs = entry.getValue();
			for (String docId : docIDs) {
				String clusterID = gsDocIDToClusterID.get(docId);
				switch (clusterID) {
				case BUSINESS:
					businessCount.right++;
					break;
				case ENTERTAINMENT:
					entertainmentCount.right++;
					break;
				case POLITICS:
					politicsCount.right++;
					break;
				case SPORT:
					sportCount.right++;
					break;
				case TECH:
					techCount.right++;
					break;
				};
			}
			
			MutablePair<String,Integer> dominantClass = Stream.of(businessCount, entertainmentCount, politicsCount, sportCount, techCount)
					.max((e1, e2) -> Integer.compare(e1.right, e2.right)).get();
			Integer clusterSize = docIDs.size();
			double purity = dominantClass.right / (double)clusterSize;
			System.out.println("Purity of cluster " + clusterNum + " is: " +  purity + "." + " Domonant Class is: " + dominantClass.left);						
		});
		
		// Calculating the RI (Rand Index).
		// Convert the  from ClusterNumber to DocIDs Map to a list of pairs (Pair<ClusterNum, DocId).
		List<ImmutablePair<Integer, String>> docIdsWithCluster = new ArrayList<>();
		clusterNumToDocIDs.entrySet().stream().forEach(entry -> {
			// Associate to each DocId it's cluster number and create a pair list. All to the accumulated list.
			Integer clusterNum = entry.getKey();
			List<String> docIDs = entry.getValue();
			List<ImmutablePair<Integer, String>> docIdsInfo = docIDs.stream()
				.map(docId -> new ImmutablePair<Integer, String>(clusterNum, docId))
				.collect(Collectors.toCollection(ArrayList::new));
			
			docIdsWithCluster.addAll(docIdsInfo);
		});
		
		/**
         *  The number of pairs of points with the same class in the gold standard and also assigned to the same cluster in K.
         */
        int a = 0; 
        /**
         * The number of pairs of points with the same class in the gold standard, but in different clusters in K.
         */
        int b = 0;
        /**
         * The number of pairs of points with the same cluster in K, but with a different class in the gold standard.
         */
        int c = 0;
        /**
         * The number of pairs of points with different class in the gold standard and a different cluster in K.
         */
        int d = 0;
        
        int numOfDocs = docIdsWithCluster.size();
        ImmutablePair<Integer, String> clousterDoc1 = null;
        ImmutablePair<Integer, String> clousterDoc2 = null;
        for (int i = 0; i < numOfDocs - 1; i++) {
        	for (int j = i + 1; j < numOfDocs; j++) {
        		clousterDoc1 = docIdsWithCluster.get(i);
        		clousterDoc2 = docIdsWithCluster.get(j);

        		String clusterID1 = gsDocIDToClusterID.get(clousterDoc1.getValue());
        		String clusterID2 = gsDocIDToClusterID.get(clousterDoc2.getValue());
        		if (clusterID1.equals(clusterID2)) {
        			// Points have the same class in the gold standard. 
        			if (clousterDoc1.getKey() == clousterDoc2.getKey()) {
        				// Points assigned to same cluster.
        				a++;
        			} else {
        				// Points are in a  different clusters in K.
        				b++;
        			}
        		}
        		else {
        			// Points have a different class in the gold standard.
        			if (clousterDoc1.getKey() == clousterDoc2.getKey()) {
        				// Points assigned to same cluster.
        				c++;
        			}
        			else {
        				// Points are with a different class in the gold standard and a different cluster.
        				d++;

        			}
        		}
        	}
        }
        
        float randIndex = ((float) (a + d)) / ((float) (a + b + c + d));
        System.out.println("Rand Index: " + randIndex + ".");
	}

	private static Map<Integer, List<String>> createClusterNumToDocIDsMap(String clusteringOutputFile) {
		Map<Integer, List<String>> clusterNumToDocIDs = null;
		Path clusteringOutputPath = Paths.get(clusteringOutputFile);
		try {
			clusterNumToDocIDs = Files.lines(clusteringOutputPath)
					.map (line -> {
						String[] columns = line.split(",");
						String docId = columns[0];
						Integer clusterNum = Integer.parseInt(columns[1]);						
						return new ImmutablePair<Integer, String>(clusterNum, docId);
					})
					.collect(Collectors.groupingBy(entry -> entry.getKey(), 
							Collectors.mapping((ImmutablePair<Integer, String> entry) -> entry.getValue(),
									Collectors.toList())));
		} catch (IOException e) {
			// None to do. return value will be null.
		}

		return clusterNumToDocIDs;
	}

	private static Map<String, String> createDocIDToClusterIDMap(String gsRootDir) {
		Map<String, String> docIdToClusterId = new HashMap<>(); 
		// Parse and collect all documents. Assuming that the first hierarchy after the root directory are the clusters.
		try {
			Files.walk(Paths.get(gsRootDir)).forEach(folderPath -> {
				if (Files.isDirectory(folderPath) && !gsRootDir.equals(folderPath.getFileName().toString())) {
					// New cluster to process. Parse all files of the cluster to Documents.
					try {
						Files.walk(folderPath).forEach(filePath -> {
							if (Files.isRegularFile(filePath)) {
								String fileName = FilenameUtils.removeExtension(filePath.getFileName().toString());
								// The folders with the first hierarchy after the root directory are the clusters.
								String clusterId = folderPath.getFileName().toString();
								String docId = clusterId + fileName;
								docIdToClusterId.put(docId, clusterId);
							}						
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			});
		} catch (Exception e) {
			return null;
		}

		return docIdToClusterId;
	}

}
