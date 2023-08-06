class FindResult {
  node: ChildNode
  childIndex: number

  constructor(n: ChildNode, chIndex: number = -1) {
    this.node = n
    this.childIndex = chIndex
  }
}