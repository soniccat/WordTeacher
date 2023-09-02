import { FoundResult } from "./foundresult"
/*
// DOM parser with a cursor
// cursor - a child position of the current parent node, we move cursor down the siblings when calling find.. methods
// to change the parent we call go.. methods, in this case we keep a stack of cursor positions, so a cursor position is restored after goOut
dw = DOMWalker()
  .findNodeWithClass("tool__correct") // moves cursor to the node with a class
  .goIn() // go deeper, starts searching through cursor's childs (goOut to go back)
  .findNodeContainingText("80 common phrasal verbs") // moves cursor to the node containing text
  .textContent { text -> jb.set("title", text) } // extract cursor's text and set it to json title property
  .call { jb.startArray("cards") }
  .splitByFunction( // splitByFunctionWithDOMWalker
    dw.findNodeWithClassSplitter("tool__number"),
    { node (cursor's parent), index (child index), length (in sibling count) -> 
      cardDw = DOMWalker(node, index, lenght)
        .findNodeWithClass("tool__number")
        .textContent { text -> jb.set("title", text.trimmed()) }
        .nextSibling() // go to the next sibling
        .textContent { text -> jb.set("definition", text) } 
    }
  )
  .call { jb.endArray() }
*/
export class DOMWalker {
  private cursorStack: Array<DOMWalkerCursor> = []
  private cursor: DOMWalkerCursor
  private lastFoundResult?: FoundResult = null
  private internalDOMWalker?: DOMWalker = null
  private stateStack: Array<DOMWalkerState> = []

  constructor(cursor: DOMWalkerCursor) {
    this.cursor = cursor
  }

  reset(cursor: DOMWalkerCursor) {
    this.cursorStack = []
    this.cursor = cursor
    this.lastFoundResult = null
    this.internalDOMWalker = null
  }

  pushState(): DOMWalker {
    this.stateStack.push(new DOMWalkerState(this.cursorStack, this.cursor, this.lastFoundResult))

    return this
  }

  popState(): DOMWalker {
    const state = this.stateStack.pop()
    this.cursorStack = state.cursorStack
    this.cursor = state.cursor
    this.lastFoundResult = state.lastFoundResult

    return this
  }

  goIn(): DOMWalker {
    const c = this.cursor
    this.cursorStack.push(this.cursor)
    this.cursor = c.inCursor()

    return this
  }

  goToFoundResult(): DOMWalker {
    if (this.lastFoundResult == null) {
      throw new DOMWalkerError(`goIn() failed as lastFoundResult is null`)
    }

    this.cursorStack.push(this.cursor)
    this.cursor = new DOMWalkerCursor(this.lastFoundResult.node, this.lastFoundResult.childIndex)

    return this
  }

  goInFoundResult(): DOMWalker {
    if (this.lastFoundResult == null) {
      throw new DOMWalkerError(`goIn() failed as lastFoundResult is null`)
    }

    this.cursorStack.push(this.cursor)
    this.cursor = new DOMWalkerCursor(this.lastFoundResult.childNode(), 0)

    return this
  }

  goOut(): DOMWalker {
    this.cursor = this.cursorStack.pop()

    return this
  }

  nextSibling(): DOMWalker {
    if (this.cursor.childIndex + 1 == this.cursor.node.childNodes.length) {
      throw new DOMWalkerError("nextSibling is called at the last node")
    }
    this.cursor.childIndex += 1

    return this
  }

  goToNodeWithClass(name: string): DOMWalker {
    this.findNodeWithClass(name)
    this.goToFoundResult()

    return this
  }

  findNodeWithClass(name: string): DOMWalker {
    const fr = findNodeWithClass(this.cursor.node, this.cursor.range(), name)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeWithClass(${name}) failed at ${this.cursor.toString()}`)
    }

    return this
  }

  findNodeContainingText(text: string): DOMWalker {
    const fr = findNodeContainingText(this.cursor.node, this.cursor.range(), text)
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeContainingText(${text}) failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  findNodeWithNotEmptyText(): DOMWalker {
    const fr = findNodeWithNotEmptyText(this.cursor.node, this.cursor.range())
    if (fr != null) {
      this.lastFoundResult = fr
      this.cursor.childIndex = fr.childIndexInOriginalNode
    } else {
      throw new DOMWalkerError(`findNodeWithNotEmptyText() failed at ${this.cursor.toString()}`)
    }
    
    return this
  }

  splitByFunctionWithDOMWalker(f: (node: Node, range: DOMWalkerRange) => FoundResult | null, itemCallback: (dw: DOMWalker) => void): DOMWalker {
    const internalDOMWalker = this.requireInternalDOMWalker(this.cursor)
    const foundResults = splitByFunction(this.cursor.node, this.cursor.range(), f)

    for (let index = 0; index < foundResults.length; index++) {
      const fr = foundResults[index]

      if (index == foundResults.length - 1) {
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, -1))
      } else {
        const nextFr = foundResults[index+1]
        internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, nextFr.childIndexInOriginalNode))
      }
      itemCallback(internalDOMWalker)
    }

    return this
  }

  textContent(f: (text: string) => void ): DOMWalker {
    f(this.cursor.childNode().textContent)
    return this
  }

  childNode(f: (text: Node) => void ): DOMWalker {
    f(this.cursor.childNode())
    return this
  }

  call(f: () => void): DOMWalker {
    f()
    return this
  }

  try(f: (dw: DOMWalker) => void): DOMWalker {
    try {
      f(this)
    } catch (e) {
      console.log(e)
    }

    return this
  }

  whileNotEnd(f: () => void): DOMWalker {
    const p = this.cursor.childIndex
    try {
      while (!this.cursor.atEnd()) {
        f()
        if (p == this.cursor.childIndex) {
          break
        }
      }
    } catch (e) {
      if (!(e instanceof DOMWalkerError)) {
        console.log(e)
      }
    }

    return this
  }

  requireInternalDOMWalker(cursor: DOMWalkerCursor): DOMWalker {
    if (this.internalDOMWalker == null) {
      this.internalDOMWalker = new DOMWalker(cursor)
    }

    return this.internalDOMWalker
  }
}

export class DOMWalkerCursor {
  node: Node
  childIndex = -1
  maxIndex = -1

  constructor(node: Node);
  constructor(node: Node, childIndex: number);
  constructor(node: Node, childIndex?: number, maxIndex?: number);
  constructor(node: Node, childIndex?: number, maxIndex?: number) {
    this.node = node

    if (childIndex != null) {
      this.childIndex = childIndex
    }

    if (maxIndex != null) {
      this.maxIndex = maxIndex
    }
  }

  childNode(): Node | null {
    if (this.childIndex != -1) {
      return this.node.childNodes[this.childIndex]
    } else {
      return null
    }
  }

  inCursor(): DOMWalkerCursor {
    return new DOMWalkerCursor(this.node.childNodes[this.childIndex], 0)
  }

  toString(): string {
    return `node: ${this.node.textContent}\nchildIndex: ${this.childIndex}\nmaxIndex: ${this.maxIndex}`
  }

  range(): DOMWalkerRange {
    return new DOMWalkerRange(this.childIndex, this.maxIndex)
  }

  atEnd(): boolean {
    return this.maxIndex != -1 && this.childIndex >= this.maxIndex || this.childIndex >= this.node.childNodes.length
  }
}

class DOMWalkerState {
  cursorStack: Array<DOMWalkerCursor> = []
  cursor: DOMWalkerCursor
  lastFoundResult?: FoundResult = null

  constructor(cursorStack: Array<DOMWalkerCursor>, cursor: DOMWalkerCursor, lastFoundResult?: FoundResult) {
    this.cursorStack = Array(...cursorStack)
    this.cursor = new DOMWalkerCursor(cursor.node, cursor.childIndex, cursor.maxIndex)
    this.lastFoundResult = lastFoundResult
  }
}

// [startIndex..endIndex)
// -1 for startIndex means we include the node itself
// -1 for endIndex means infinity 
export class DOMWalkerRange {
  start: number
  end: number

  constructor(start: number, end: number) {
    this.start = start
    this.end = end
  }

  isInRange(v: number): boolean {
    return v >= this.start && (this.end == -1 || v < this.end)
  }

  rangeWithStart(start: number): DOMWalkerRange {
    return new DOMWalkerRange(start, this.end)
  }

  rangeWithEnd(end: number): DOMWalkerRange {
    return new DOMWalkerRange(this.start, end)
  }
}

const NodeRange = new DOMWalkerRange(-1, -1)

export class DOMWalkerError extends Error {
  constructor(s: string) {
    super(s)
    Object.setPrototypeOf(this, DOMWalkerError.prototype);
  }
}

//// functions

function splitByFunction(node: Node, range: DOMWalkerRange, f: (aNode: Node, aRange: DOMWalkerRange) => FoundResult | null): Array<FoundResult> {
  const result = Array<FoundResult>()
  for (let index = range.start; index < node.childNodes.length && range.isInRange(index);) {
    const fr = f(node, new DOMWalkerRange(index, range.end))
    if (fr != null) {
      result.push(fr)
      index = fr.childIndexInOriginalNode + 1 
    } else {
      ++index
    }
  }

  return result
}

export function findNodeWithClassSplitter(className: string): (aStartNode: Node, range: DOMWalkerRange) => FoundResult | null {
  return (aStartNode: Node, range: DOMWalkerRange) => {
    return findNodeWithClass(aStartNode, range, className)
  }
}

function findNodeWithClass(startNode: Node, range: DOMWalkerRange, className: string): FoundResult | null {
  if (range.start == -1 && startNode instanceof Element) {
    const v = startNode.attributes.getNamedItem("class")
    if (v instanceof Attr) {
      if (v.value.trim() == className) {
        return new FoundResult(startNode)
      }
    }
  }

  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      const fr = findNodeWithClass(element, NodeRange, className)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  return null
}

function findNodeContainingText(startNode: Node, range: DOMWalkerRange, text: string): FoundResult | null {
  return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.indexOf(text) != -1 } )
}

function findNodeWithNotEmptyText(startNode: Node, range: DOMWalkerRange): FoundResult | null {
  return findNodeContainingWithTextChecker(startNode, range, (t: string) => { return t.trim().length > 0 } )
}

function findNodeContainingWithTextChecker(startNode: Node, range: DOMWalkerRange, textChecker: (t: string) => boolean): FoundResult | null {
  if (startNode instanceof Node) {
    for (let index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
      const element = startNode.childNodes[index];
      const fr = findNodeContainingWithTextChecker(element, NodeRange, textChecker)
      if (fr != null) {
        if (fr.childIndex == -1) {
          return new FoundResult(startNode, index, index)
        }

        fr.childIndexInOriginalNode = index
        return fr
      }
    };
  }

  if (range.start == -1 && startNode instanceof Node) {
    if (textChecker(startNode.textContent)) {
      return new FoundResult(startNode)
    }
  }

  return null
}

export function cutFirstWord(str: string): string {
  const i = str.indexOf(' ')
  if (i != -1) {
    return str.substring(i+1)
  }

  return str
}
