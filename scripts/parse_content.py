#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 app/src/main/assets/index.html 解析成结构化 JSON（app/src/main/assets/content.json）。

只用 Python 3 标准库（html.parser），构建期跑一次即可。
解析失败（任何断言不通过）时非零退出，避免把坏数据打进 APK。
"""

import json
import os
import re
import sys

try:  # Python 3.4+
    from html.parser import HTMLParser
except ImportError:  # pragma: no cover
    from HTMLParser import HTMLParser

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "assets", "index.html")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "content.json")

# 期望值：解析结果与上游内容必须完全一致，否则说明结构变了
EXPECT_SECTIONS = 34
EXPECT_CARDS = 650
EXPECT_GRADES = {"A": 428, "B": 171, "C": 51}
EXPECT_RATIOS = {"极高": 111, "高": 294, "一般": 245}
EXPECT_TAG_KEYS = ["口径", "钱", "时间", "毅力", "收益"]
EXPECT_FIELDS = ["成本", "收益", "备注"]
# 卡片内（.sbody）的 http/https 文献链接总数；文末 footer 的 1 条不计在内
EXPECT_SOURCE_LINKS = 1536
# 全文 http/https 链接总数（含 footer 的数据来源链接）
EXPECT_TOTAL_HTTP_LINKS = 1537

VOID = {
    "area", "base", "br", "col", "embed", "hr", "img", "input",
    "link", "meta", "param", "source", "track", "wbr",
}

WS = re.compile(r"\s+")


def norm(s):
    """折叠空白并去掉首尾空格。"""
    return WS.sub(" ", s or "").strip()


class Node(object):
    __slots__ = ("tag", "attrs", "children", "parent", "data")

    def __init__(self, tag, attrs=None, parent=None, data=None):
        self.tag = tag
        self.attrs = attrs or {}
        self.children = []
        self.parent = parent
        self.data = data


class DomBuilder(HTMLParser):
    """极简 DOM：只保留标签、属性和文本，够解析这本书就行。"""

    def __init__(self):
        HTMLParser.__init__(self, convert_charrefs=True)
        self.root = Node("#root")
        self.stack = [self.root]

    def handle_starttag(self, tag, attrs):
        node = Node(tag, dict(attrs), self.stack[-1])
        self.stack[-1].children.append(node)
        if tag not in VOID:
            self.stack.append(node)

    def handle_startendtag(self, tag, attrs):
        self.stack[-1].children.append(Node(tag, dict(attrs), self.stack[-1]))

    def handle_endtag(self, tag):
        if tag in VOID:
            return
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag:
                del self.stack[i:]
                return
        # 没有匹配的开标签：忽略

    def handle_data(self, data):
        self.stack[-1].children.append(
            Node("#text", parent=self.stack[-1], data=data)
        )


def text_of(node):
    parts = []

    def walk(n):
        if n.data is not None:
            parts.append(n.data)
            return
        for c in n.children:
            walk(c)

    walk(node)
    return norm("".join(parts))


def has_class(node, name):
    return name in (node.attrs.get("class") or "").split()


def iter_nodes(node):
    stack = [node]
    while stack:
        n = stack.pop()
        yield n
        stack.extend(reversed(n.children))


def find_all(node, tag=None, cls=None):
    out = []
    for n in iter_nodes(node):
        if n is node:
            continue
        if tag is not None and n.tag != tag:
            continue
        if cls is not None and not has_class(n, cls):
            continue
        out.append(n)
    return out


def find_first(node, tag=None, cls=None):
    for n in iter_nodes(node):
        if n is node:
            continue
        if tag is not None and n.tag != tag:
            continue
        if cls is not None and not has_class(n, cls):
            continue
        return n
    return None


def children_by_tag(node, tag):
    return [c for c in node.children if c.tag == tag]


# 短括号补注（跟在 </a> 后面、其实属于上一条文献），如「（北京市卫生健康委员会转载）」
PAREN_NOTE = re.compile(r"^（[^）]{0,60}）\s*[；;]?\s*")

# 分隔符落在下一条文献开头的残留：` ; WHO (2025)...` / `；深圳市...`
LEAD_SEP = re.compile(r"^[\s;；]+")
TAIL_SEP = re.compile(r"[\s;；]+$")


def clean_sep(s):
    return TAIL_SEP.sub("", LEAD_SEP.sub("", s or ""))


def extract_sources(sbody):
    """把 .sbody 拆成 [{text, url}]。

    上游并没有稳定的分隔符：` ; `、`；` 都出现过，而且 `；` 还会出现在
    「」引用条文中间、`;` 会出现在 URL 查询串里。所以这里以 <a> 为锚点切分：
    每遇到一个链接就收束上一条文献，链接之间的文本归属下一条文献，
    紧跟链接的短括号补注回填给上一条。
    """
    events = []

    def walk(n):
        if n.data is not None:
            events.append(("t", n.data))
            return
        if n.tag == "a":
            events.append(("a", n.attrs.get("href") or ""))
            return
        for c in n.children:
            walk(c)

    walk(sbody)

    sources = []
    buf = []

    def flush(url):
        text = clean_sep(norm("".join(buf)))
        del buf[:]
        if text or url:
            sources.append({"text": text, "url": url or None})

    for kind, val in events:
        if kind == "t":
            buf.append(val)
        else:
            flush(val)

    tail = clean_sep(norm("".join(buf)))
    if tail:
        if sources:
            prev = sources[-1]["text"]
            sources[-1]["text"] = (prev + " " + tail).strip() if prev else tail
        else:
            sources.append({"text": tail, "url": None})

    # 回填短括号补注
    for i in range(1, len(sources)):
        m = PAREN_NOTE.match(sources[i]["text"])
        if not m:
            continue
        note = m.group(0).strip().rstrip("；;").strip()
        rest = sources[i]["text"][m.end():].strip()
        if not note:
            continue
        prev = sources[i - 1]["text"]
        sources[i - 1]["text"] = (prev + " " + note).strip() if prev else note
        sources[i]["text"] = rest

    out = []
    for s in sources:
        text = clean_sep(s["text"])
        if not text and not s["url"]:
            continue
        out.append({"text": text, "url": s["url"]})
    return out


def parse(path):
    with open(path, "r", encoding="utf-8") as f:
        html = f.read()

    builder = DomBuilder()
    builder.feed(html)
    builder.close()
    root = builder.root

    main = find_first(root, "main")
    if main is None:
        die("找不到 <main> 节点")

    sections = []
    cards = []
    for sec in find_all(main, "section"):
        sec_id = sec.attrs.get("id") or ""
        m = re.fullmatch(r"sec(\d+)", sec_id)
        if not m:
            continue
        num = int(m.group(1))

        head = find_first(sec, "div", "sec-h")
        if head is None:
            die("section %s 缺少 .sec-h" % sec_id)
        h2 = find_first(head, "h2")
        meta = find_first(head, "span", "meta")
        title = text_of(h2) if h2 is not None else ""
        title = re.sub(r"^\d+\.\s*", "", title)
        meta_text = text_of(meta) if meta is not None else ""
        mm = re.search(r"(\d+)", meta_text)
        count = int(mm.group(1)) if mm else 0

        intro_el = find_first(sec, "p", "intro")
        intro = text_of(intro_el) if intro_el is not None else ""

        sections.append({
            "id": sec_id,
            "num": num,
            "title": title,
            "intro": intro,
            "count": count,
        })

        for art in find_all(sec, "article", "card"):
            cards.append(parse_card(art, sec_id, num))

    return sections, cards, html


def parse_card(art, sec_id, sec_num):
    card_id = art.attrs.get("id") or ""
    m = re.fullmatch(r"s(\d+)-(\d+)", card_id)
    if not m:
        die("卡片 id 非法：%r" % card_id)
    if int(m.group(1)) != sec_num:
        die("卡片 %s 的节号与所在 section %s 不一致" % (card_id, sec_id))

    num_el = find_first(art, "span", "num")
    h3 = find_first(art, "h3")
    if h3 is None:
        die("卡片 %s 缺少标题" % card_id)
    num_text = text_of(num_el) if num_el is not None else m.group(2)
    if num_text != m.group(2):
        die("卡片 %s 的 .num(%s) 与 id 不一致" % (card_id, num_text))

    chips = find_first(art, "div", "chips")
    if chips is None:
        die("卡片 %s 缺少 .chips" % card_id)
    tags = {}
    for tag_el in find_all(chips, "span", "tag"):
        raw = text_of(tag_el)
        parts = raw.split(" ", 1)
        if len(parts) != 2:
            die("卡片 %s 的 tag 结构异常：%r" % (card_id, raw))
        tags[parts[0]] = parts[1].strip()

    plain_el = find_first(art, "p", "plain")
    if plain_el is None:
        die("卡片 %s 缺少 .plain" % card_id)
    plain = text_of(plain_el)

    fields = {}
    fields_el = find_first(art, "div", "fields")
    if fields_el is None:
        die("卡片 %s 缺少 .fields" % card_id)
    for f in find_all(fields_el, "div", "f"):
        b = find_first(f, "b")
        if b is None:
            die("卡片 %s 的 .f 缺少标签" % card_id)
        label = text_of(b)
        value = text_of(f)
        if value.startswith(label):
            value = value[len(label):].strip()
        fields[label] = value

    sbody = find_first(art, "div", "sbody")
    sources = extract_sources(sbody) if sbody is not None else []

    return {
        "id": card_id,
        "secId": sec_id,
        "num": int(m.group(2)),
        "title": text_of(h3),
        "grade": art.attrs.get("data-grade") or "",
        "ratio": art.attrs.get("data-ratio") or "",
        "tags": tags,
        "plain": plain,
        "cost": fields.get("成本", ""),
        "benefit": fields.get("收益", ""),
        "note": fields.get("备注", ""),
        "sources": sources,
    }


def die(msg, *args):
    if args:
        msg = msg % args
    sys.stderr.write("ERROR: %s\n" % msg)
    sys.exit(1)


def check(cond, msg):
    if not cond:
        die(msg)


def main():
    if not os.path.exists(SRC):
        die("找不到源文件 %s（先跑 scripts/fetch-content.sh）" % SRC)

    sections, cards, html = parse(SRC)

    # ---- 断言 ----
    check(len(sections) == EXPECT_SECTIONS,
          "节数 %d != %d" % (len(sections), EXPECT_SECTIONS))
    check([s["num"] for s in sections] == list(range(1, EXPECT_SECTIONS + 1)),
          "节号不是 1..34 连续")
    for s in sections:
        check(s["title"], "节 %s 标题为空" % s["id"])
        check(s["intro"], "节 %s 导语为空" % s["id"])
        check(s["count"] > 0, "节 %s 条数解析失败" % s["id"])

    check(len(cards) == EXPECT_CARDS, "卡片数 %d != %d" % (len(cards), EXPECT_CARDS))

    ids = [c["id"] for c in cards]
    check(len(set(ids)) == len(ids), "卡片 id 有重复")

    sec_ids = {s["id"] for s in sections}
    sec_counts = {s["id"]: 0 for s in sections}
    for c in cards:
        check(c["secId"] in sec_ids, "卡片 %s 的 secId 不存在" % c["id"])
        sec_counts[c["secId"]] += 1
    for s in sections:
        check(sec_counts[s["id"]] == s["count"],
              "节 %s 实际 %d 条，meta 写 %d 条"
              % (s["id"], sec_counts[s["id"]], s["count"]))

    grades = {}
    ratios = {}
    for c in cards:
        grades[c["grade"]] = grades.get(c["grade"], 0) + 1
        ratios[c["ratio"]] = ratios.get(c["ratio"], 0) + 1
        check(list(c["tags"].keys()) == EXPECT_TAG_KEYS,
              "卡片 %s 的 tags 键不是 %s：%s" % (c["id"], EXPECT_TAG_KEYS, list(c["tags"].keys())))
        check(all(v for v in c["tags"].values()), "卡片 %s 有空 tag 值" % c["id"])
        check(c["plain"], "卡片 %s 的 plain 为空" % c["id"])
        check(c["cost"] and c["benefit"] and c["note"],
              "卡片 %s 的 fields 不完整" % c["id"])
    check(grades == EXPECT_GRADES, "等级分布 %s != %s" % (grades, EXPECT_GRADES))
    check(ratios == EXPECT_RATIOS, "性价比分布 %s != %s" % (ratios, EXPECT_RATIOS))

    src_links = sum(1 for c in cards for s in c["sources"] if s["url"])
    check(src_links == EXPECT_SOURCE_LINKS,
          "文献外链 %d != %d" % (src_links, EXPECT_SOURCE_LINKS))
    total_http = len(re.findall(r'<a\s+href="https?://', html))
    check(total_http == EXPECT_TOTAL_HTTP_LINKS,
          "全文 http 链接 %d != %d" % (total_http, EXPECT_TOTAL_HTTP_LINKS))

    no_src = [c["id"] for c in cards if not c["sources"]]
    check(not no_src, "有卡片解析不出任何来源：%s" % no_src[:5])

    doc = {"sections": sections, "cards": cards}
    payload = json.dumps(doc, ensure_ascii=False, separators=(",", ":"))
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(payload)

    # ---- 统计 ----
    sizes = sorted(len(c["sources"]) for c in cards)
    print("OK  sections=%d cards=%d" % (len(sections), len(cards)))
    print("    grades  %s" % json.dumps(grades, ensure_ascii=False))
    print("    ratios  %s" % json.dumps(ratios, ensure_ascii=False))
    print("    sources total=%d  with-url=%d  max=%d  无链接卡片=%d"
          % (sum(sizes), src_links, sizes[-1],
             sum(1 for c in cards if all(not s["url"] for s in c["sources"]))))
    print("    links(全文 http)=%d" % total_http)
    print("    out %s  (%d bytes)" % (os.path.relpath(OUT, ROOT), len(payload.encode("utf-8"))))


if __name__ == "__main__":
    main()
