import java.io.{PrintWriter, FileWriter}

import org.apache.spark.SparkContext
import org.apache.spark.graphx.{Edge, Graph}
import org.apache.spark.rdd.RDD

/**
 * @author Kontopoulos Ioannis
 */
class NGramGraphStorage(val sc: SparkContext) extends GraphStorage {

  /**
   * Save vertices ans edges of graph to files
   * NOTE: if graph with certain label has already been saved the files will be appended
   * therefore the saved edges and vertices will not correspond to the original graph
   * @param g graph to save
   * @param label label of saved graph
   */
  override def saveGraph(g: Graph[String, Double], label: String) = {
    //collect edges per partition, so there is no memory overflow
    val ew = new FileWriter(label + "Edges.txt", true)
    val edgeParts = g.edges.distinct.partitions
    for (p <- edgeParts) {
      val idx = p.index
      //The second argument is true to avoid rdd reshuffling
      val partRdd = g.edges.distinct
        .mapPartitionsWithIndex((index: Int, it: Iterator[Edge[Double]]) => if(index == idx) it else Iterator(), true )
      //partRdd contains all values from a single partition
      partRdd.collect.foreach{ e =>
        try {
          ew.write(e.srcId + "<>" + e.dstId + "<>" + e.attr + "\n")
        }
        catch {
          case ex: Exception => {
            println("Could not write to file. Reason: " + ex.getMessage)
          }
        }
      }
    }
    //close file
    ew.close
    //collect vertices per partition, so there is no memory overflow
    val vw = new FileWriter(label + "Vertices.txt", true)
    val vertexParts = g.vertices.distinct.partitions
    for (p <- vertexParts) {
      val idx = p.index
      //The second argument is true to avoid rdd reshuffling
      val partRdd = g.vertices.distinct
        .mapPartitionsWithIndex((index: Int, it: Iterator[(Long, String)]) => if(index == idx) it else Iterator(), true )
      //partRdd contains all values from a single partition
      partRdd.collect.foreach{ v =>
        try {
          vw.write(v._1 + "<>" + v._2.replaceAll("\n", " ") + "\n")
        }
        catch {
          case ex: Exception => {
            println("Could not write to file. Reason: " + ex.getMessage)
          }
        }
      }
    }
    //close file
    vw.close
  }

  /**
   * Load graph from edges file and vertices file
   * @param label label of saved graph
   * @return graph
   */
  override def loadGraph(label: String): Graph[String, Double] = {
    //path for vertices file
    val vertexFile = label + "Vertices.txt"
    //path for edges file
    val edgeFile = label + "Edges.txt"
    //create EdgeRDD from file rows
    val edges: RDD[Edge[Double]] = sc.textFile(edgeFile).map{ line =>
      val row = line.split("<>")
      Edge(row(0).toLong, row(1).toLong, row(2).toDouble)
    }
    //create VertexRDD from file rows
    val vertices: RDD[(Long, String)] = sc.textFile(vertexFile).map{ line =>
      val row = line.split("<>")
      (row(0).toLong, row(1))
    }
    //create graph
    val graph: Graph[String, Double] = Graph(vertices, edges)
    graph
  }

  /**
   * Save graph to dot format file
   * NOTE: use this method only for small graphs and testing purposes,
   * because it fetches all the data to the driver program,
   * leading to memory overflow if the graph is too large
   * @param g graph to save
   */
  def saveGraphToDotFormat(g: Graph[String, Double]) = {
    var str = "digraph nGramGraph {\n"
    //map that holds the vertices
    var vertices: Map[Int, String] = Map()
    //collect vertices from graph and replace punctuations
    g.vertices.collect
      .foreach{ v => vertices += ( v._1.toInt -> v._2.replaceAll("[`~!@#$%^&*()+-=,.<>/?;:' ]", "_")) }
    //construct the string
    g.edges.distinct.collect
      .foreach{ e => str += "\t" + vertices(e.srcId.toInt) + " -> " + vertices(e.dstId.toInt) + " [label=\"" + e.srcId + "" + e.dstId + "\" weight=" + e.attr + "];\n" }
    str += "}"
    //write string to file
    try {
      Some(new PrintWriter("nGramGraph.dot")).foreach{p => p.write(str); p.close}
    }
    catch {
      case ex: Exception => {
        println("Could not write to file. Reason: " + ex.getMessage)
      }
    }
  }

}