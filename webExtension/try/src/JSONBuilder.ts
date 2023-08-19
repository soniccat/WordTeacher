
// JSON Builder with a cursor
// select(path like "obj.obj")
// startObject/endObject/set
// startArray/endArray/add,
export class JSONBuilder {
  json: any = {}
  cursor: any = this.json
  private cursorStack: Array<object> = Array()

  constructor();
  constructor(json?: object);
  constructor(json?: object, cursor?: object);
  constructor(json?: object, cursor?: object) {
    if (json != null) {
      this.json = json
    }

    if (cursor != null) {
      this.cursor = cursor
    }
  }

  set(key: string, v: any): JSONBuilder {
    this.cursor[key] = v

    return this
  }

  startObject(name: string): JSONBuilder {
    this.cursorStack.push(this.cursor)
    let oldCursor = this.cursor
    this.cursor = {}

    oldCursor[name] = this.cursor

    return this
  }

  startArrayObject(): JSONBuilder {
    this.cursorStack.push(this.cursor)
    let obj = {}
    this.push(obj)
    this.cursor = obj

    return this
  }

  endObject(): JSONBuilder {
    this.cursor = this.cursorStack.pop()

    return this
  }

  startArray(name: string): JSONBuilder {
    this.cursorStack.push(this.cursor)
    let oldCursor = this.cursor
    this.cursor = []
    oldCursor[name] = this.cursor

    return this
  }

  endArray(): JSONBuilder {
    this.endObject()

    return this
  }

  push(v: any): JSONBuilder {
    if (!Array.isArray(this.cursor)) {
      throw new JSONBuilderError(`push failed as the cursor isn't array in ${JSON.stringify(this.json)}`)
    }
    this.cursor.push(v)

    return this
  }

  select(path: [string]): JSONBuilder {
    var c = this.json
    for (let i=0; i<path.length; ++i) {
      c = c[path[i]]
      if (c == undefined) {
        throw new JSONBuilderError(`select non existing path in in ${JSON.stringify(this.json)}`)
      }
    }
    this.cursor = c

    return this
  }
}

export class JSONBuilderError extends Error {
}
