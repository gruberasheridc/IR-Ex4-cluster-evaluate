package ir.websearch.clustering.evaluate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
			int businessCount = 0;
			int entertainmentCount = 0;
			int politicsCount = 0;
			int sportCount = 0;
			int techCount = 0;

			List<String> docIDs = entry.getValue();
			for (String docId : docIDs) {				
				String clusterID = gsDocIDToClusterID.get(docId);
				switch (clusterID) {
				case BUSINESS:
					businessCount++;
					break;
				case ENTERTAINMENT:
					entertainmentCount++;
					break;
				case POLITICS:
					politicsCount++;
					break;
				case SPORT:
					sportCount++;
					break;
				case TECH:
					techCount++;
					break;
				};
			}
		});
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
