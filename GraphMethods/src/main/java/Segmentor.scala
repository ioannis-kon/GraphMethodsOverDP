/**
 * @author Kontopoulos Ioannis
 */
trait Segmentor {

  def getComponents(e: Entity): List[Atom]

}