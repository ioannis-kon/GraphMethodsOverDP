package structs

import traits.Atom

/**
 * @author Kontopoulos Ioannis
 */
case class StringAtom(val label: Long, val dataStream: String) extends Atom
