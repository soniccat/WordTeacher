class FindResult {
  node: Node
  childIndex: number
  childIndexInOriginalNode: number

  constructor(n: Node, chIndex: number = -1, chIndexInOriginalNode: number = -1) {
    this.node = n
    this.childIndex = chIndex
    this.childIndexInOriginalNode = chIndexInOriginalNode
  }

  textContent(): String | null {
    if (this.node instanceof Element) {
      return this.node.textContent
    } else {
      return null
    }
  }

  childNodes(): NodeListOf<Node> | null{
    let n = this.node
    if (n instanceof Node) {
      return n.childNodes
    } else {
      return null
    }
  }
}