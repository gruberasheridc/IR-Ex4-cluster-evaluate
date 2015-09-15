package ir.websearch.clustering.evaluate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

public class EvaluateClustering {

	private static final String NEW_LINE = System.lineSeparator(); 
	private static final String QUERY_PREFIX = "q";
	private static final String DOCUMENT_PREFIX = "doc";
	private static final String DUMMAY_PREFIX = "dummy";

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
	}

	private static Map<Integer, List<String>> createClusterNumToDocIDsMap(String clusteringOutputFile) {
		// TODO Auto-generated method stub
		return null;
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
