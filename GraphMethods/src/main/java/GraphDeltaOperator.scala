import org.apache.spark.graphx.{Edge, Graph}

/**
 * @author Kontopoulos Ioannis
 */
class GraphDeltaOperator extends BinaryGraphOperator with Serializable {

  /**
   * Creates a graph that contains edges from the first graph that do not exist in the second graph
   * @param g1 first graph
   * @param g2 second graph
   * @return graph
   */
  override def getResult(g1: Graph[String, Double], g2: Graph[String, Double]): Graph[String, Double] = {
    //pair edges to only the vertices pair
    def edgeToPair (e: Edge[Double]) = ((e.srcId, e.dstId))
    //pair edges so the common edges are the ones with same vertices pair
    def edgeAttrToPair (e: Edge[Double]) = ((e.srcId, e.dstId), e.attr)
    val pairs1 = g1.edges.map(edgeToPair)
    val pairs2 = g2.edges.map(edgeToPair)
    //get the edges from first graph that do not exist in the second graph
    val emptyEdges = pairs1.subtract(pairs2)
      .map{ case (srcId, dstId) => ((srcId, dstId),  0.0) }
    //get edges with their attributes
    val RDDWithValues = g1.edges.map(edgeAttrToPair)
    //get the common edges between them
    val edges = RDDWithValues.join(emptyEdges)
      .map{ case ((srcId, dstId), (value1, value2)) => Edge(srcId, dstId, value1) }
    val vertices = g1.vertices.union(g2.vertices).distinct
    //create new graph
    val deltaGraph = Graph(vertices, edges)
    deltaGraph
  }

}