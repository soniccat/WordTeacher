export class FoundResult {
  node: Node
  childIndex: number
  childIndexInOriginalNode: number

  constructor(n: Node, chIndex = -1, chIndexInOriginalNode = -1) {
    this.node = n
    this.childIndex = chIndex
    this.childIndexInOriginalNode = chIndexInOriginalNode
  }

  textContent(): string | null {
    if (this.node instanceof Element) {
      return this.node.textContent
    } else {
      return null
    }
  }

  childNodes(): NodeListOf<Node> | null{
    const n = this.node
    if (n instanceof Node) {
      return n.childNodes
    } else {
      return null
    }
  }

  childNode(): Node {
    return this.node.childNodes[this.childIndex]
  }
}