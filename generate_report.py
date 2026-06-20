#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 ZUUL — 失落的古迹 小组实训报告 (REPORT.docx)
基于实际项目代码分析，所有数据与后端/前端源代码严格一致。
"""

from docx import Document
from docx.shared import Inches, Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
import datetime

doc = Document()

# ========== 全局样式 ==========
style = doc.styles['Normal']
font = style.font
font.name = '宋体'
font.size = Pt(11)
style.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

# 页边距
for section in doc.sections:
    section.top_margin = Cm(2.5)
    section.bottom_margin = Cm(2.5)
    section.left_margin = Cm(2.5)
    section.right_margin = Cm(2.5)


def set_cell_shading(cell, color_str):
    """设置单元格底色"""
    shading_elm = cell._tc.get_or_add_tcPr()
    shading = shading_elm.makeelement(qn('w:shd'), {
        qn('w:fill'): color_str,
        qn('w:val'): 'clear'
    })
    shading_elm.append(shading)


def make_header_row(table, row_idx, texts, color='4472C4'):
    """设置表头行样式"""
    row = table.rows[row_idx]
    for i, text in enumerate(texts):
        cell = row.cells[i]
        cell.text = ''
        p = cell.paragraphs[0]
        run = p.add_run(text)
        run.bold = True
        run.font.size = Pt(10)
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        set_cell_shading(cell, color)


def add_data_row(table, row_idx, texts, bold_first=True):
    """添加数据行"""
    row = table.rows[row_idx]
    for i, text in enumerate(texts):
        cell = row.cells[i]
        cell.text = ''
        p = cell.paragraphs[0]
        run = p.add_run(str(text))
        run.font.size = Pt(10)
        if i == 0 and bold_first:
            run.bold = True


def add_heading(text, level=1):
    h = doc.add_heading(text, level=level)
    for run in h.runs:
        run.font.name = '黑体'
        run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    return h


def add_para(text, bold=False, indent=False):
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.font.size = Pt(11)
    run.font.name = '宋体'
    run.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    if bold:
        run.bold = True
    if indent:
        p.paragraph_format.first_line_indent = Cm(0.7)
    return p


# ============================================================
#                       封 面
# ============================================================
for _ in range(4):
    doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('软件工程实训\n小组项目报告')
run.font.size = Pt(28)
run.bold = True
run.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
run.font.name = '黑体'
run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')

doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('ZUUL — 失落的古迹')
run.font.size = Pt(22)
run.font.color.rgb = RGBColor(0xC0, 0x39, 0x2B)
run.font.name = '黑体'
run.element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')

doc.add_paragraph()

cover_info = [
    ('项 目 名 称', 'ZUUL — 失落的古迹（Roguelike 地牢探险游戏）'),
    ('课 程 名 称', '软件工程实训'),
    ('指 导 教 师', '唐祖锴'),
    ('小 组 名 称', 'AAA603 小组'),
    ('学    院', '武汉理工大学 · 计算机科学与技术学院'),
]

for label, value in cover_info:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run(f'{label}：{value}')
    run.font.size = Pt(14)
    run.font.name = '宋体'
    run.element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')

doc.add_paragraph()

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run(f'报告日期：{datetime.date.today().strftime("%Y年%m月%d日")}')
run.font.size = Pt(12)
run.font.color.rgb = RGBColor(0x66, 0x66, 0x66)

doc.add_page_break()

# ============================================================
#                       目 录
# ============================================================
add_heading('目 录', level=1)
toc_items = [
    '一、项目概述',
    '二、技术栈与开发环境',
    '三、系统架构设计',
    '四、功能模块说明',
    '    4.1 随机地图生成',
    '    4.2 实时战斗系统',
    '    4.3 怪物系统',
    '    4.4 状态效果系统',
    '    4.5 背包与装备系统',
    '    4.6 商店系统',
    '    4.7 篝火祭坛系统',
    '    4.8 奇遇事件系统',
    '    4.9 铁匠强化系统',
    '    4.10 月光波技能',
    '    4.11 存档系统',
    '五、API 接口设计',
    '六、数据库设计',
    '七、小组分工与协作',
    '八、实训总结与反思',
]
for item in toc_items:
    add_para(item)

doc.add_page_break()

# ============================================================
#                   一、项目概述
# ============================================================
add_heading('一、项目概述', level=1)

add_para(
    'ZUUL — 失落的古迹 是一款基于经典文本冒险游戏 World of Zuul 扩展开发的图形化 '
    'Roguelike 地牢探险游戏。玩家扮演勇敢的探险者，穿越程序随机生成的迷宫，与神秘的 '
    '怪物战斗，收集强大的魔法物品，在篝火处恢复体力并提升能力，最终揭示隐藏在废墟中 '
    '的真相。', indent=True
)

add_para(
    '本项目为武汉理工大学软件工程实训小组协同开发任务，项目周期历时数周，经过多轮迭代 '
    '开发与重构。项目采用前后端分离架构：后端基于 Java 17 + Spring Boot 3.2.0 提供 '
    'RESTful API，前端基于 Vue 3 + Phaser 3 构建图形化游戏界面。', indent=True
)

add_para('核心玩法特点：', bold=True)
features = [
    '程序化随机地图生成，每次游戏体验独一无二',
    '三种攻击模式：扇形扫击（135°/120px）、直线突刺（21px宽）、蓄力攻击（360°/150px）',
    '四种状态效果：烧伤（3秒火焰伤害）、中毒（1秒真实伤害）、流血（攻击触发）、天使祝福（30秒全属性×1.5）',
    '五槽位装备系统：武器、护甲、披风、戒指、项链',
    '八种智慧恩赐：坚忍、浩瀚、整备、守护、强健、博学、敏捷、灵动',
    '九种注册物品：四种消耗品、两件装备、三件饰品',
    '七种房间类型：起始大厅、商店、奇遇、篝火、Boss、精英怪物、普通怪物',
    '完整的存档系统：基于 H2 数据库持久化，支持保存/读取/删除',
    '前端的 Phaser 3 图形引擎：实时战斗、粒子特效、屏幕振动、小地图导航',
]
for f in features:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(f)
    run.font.size = Pt(11)

# ============================================================
#               二、技术栈与开发环境
# ============================================================
add_heading('二、技术栈与开发环境', level=1)

add_para('2.1 后端技术栈', bold=True)
backend_items = [
    ('Java 17', '编程语言'),
    ('Spring Boot 3.2.0', 'Web 框架与依赖注入'),
    ('Spring Data JPA', 'ORM 持久化层'),
    ('H2 Database', '嵌入式关系型数据库'),
    ('Maven', '项目构建与管理'),
    ('Lombok', '代码简化（@Data、@Getter/@Setter 等）'),
]
table = doc.add_table(rows=len(backend_items) + 1, cols=2)
table.style = 'Table Grid'
table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(table, 0, ['技术', '用途'])
for idx, (tech, usage) in enumerate(backend_items):
    add_data_row(table, idx + 1, [tech, usage])

doc.add_paragraph()

add_para('2.2 前端技术栈', bold=True)
frontend_items = [
    ('Vue 3 (3.3.4)', '前端框架与组件化开发'),
    ('Phaser 3 (3.60.0)', '2D 游戏引擎（渲染、物理、动画）'),
    ('Vite (5.0.0)', '前端构建工具'),
    ('Pinia (2.0.33)', 'Vue 状态管理'),
    ('Fetch API', '与后端通信（RESTful）'),
]
table = doc.add_table(rows=len(frontend_items) + 1, cols=2)
table.style = 'Table Grid'
table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(table, 0, ['技术', '用途'])
for idx, (tech, usage) in enumerate(frontend_items):
    add_data_row(table, idx + 1, [tech, usage])

doc.add_paragraph()

add_para('2.3 开发工具', bold=True)
tool_items = [
    ('IntelliJ IDEA', '后端开发 IDE'),
    ('VS Code', '前端开发 IDE'),
    ('GitHub', '代码仓库与协同开发'),
    ('GitHub Issues', '任务管理与分配'),
    ('Postman', 'API 测试与调试'),
]
table = doc.add_table(rows=len(tool_items) + 1, cols=2)
table.style = 'Table Grid'
table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(table, 0, ['工具', '用途'])
for idx, (tech, usage) in enumerate(tool_items):
    add_data_row(table, idx + 1, [tech, usage])

doc.add_page_break()

# ============================================================
#               三、系统架构设计
# ============================================================
add_heading('三、系统架构设计', level=1)

add_para(
    '本项目采用前后端分离的 B/S 架构。前端通过 HTTP REST API 与后端通信，所有游戏逻辑在 '
    '服务端执行，前端主要负责渲染与交互。', indent=True
)

add_para('3.1 整体架构', bold=True)
arch_items = [
    '前端层（Vue 3 + Phaser 3）：负责游戏画面渲染、用户输入捕获、UI 面板展示。Phaser 3 引擎驱动游戏主画布，Vue 3 负责菜单、背包、存档等 UI 组件。',
    'API 层（Spring Boot REST Controllers）：提供 10 个 RESTful 端点，处理前端请求并返回 JSON 格式数据。',
    '服务层（GameService）：游戏核心服务，包括命令分发（11 种命令）、玩家状态注入（24+ 字段）、攻击空间命中判定（3 种算法）、地图拓扑检索。',
    '模型层：包含 Room、Player、Monster、Bag、Item、Status 等核心数据模型，以及 WisdomBoon、Altar、RandomEvent 等子系统的模型。',
    '持久层（Spring Data JPA Repository）：5 个 Repository 接口，负责游戏存档、房间状态、怪物状态、背包物品、商店状态的序列化与恢复。',
    '数据库层（H2）：嵌入式文件数据库，存储完整游戏状态。',
]
for a in arch_items:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(a)
    run.font.size = Pt(11)

add_para('3.2 后端包结构', bold=True)
pkg_items = [
    ('controller', 'GameController.java — 10个REST API端点'),
    ('service', 'GameService.java — 核心业务逻辑（命令分发/攻击判定/状态注入）\nSaveService.java — 存档序列化与恢复'),
    ('model', 'Room.java / Player.java / Monster.java / Bag.java / Item.java / ItemRegistry.java\nStatus.java / WisdomBoon.java / Magic.java / Money.java / RoomType.java\nAltar.java / AltarType.java / AttackRequest.java / RandomEvent.java\nRandomEventType.java / ShopItem.java / DroppedItem.java\nInventoryItem.java / GameResponse.java'),
    ('command', 'GoCommand.java / AttackCommand.java / BagCommand.java / ShopCommand.java\nInteractCommand.java / WaveCommand.java / PickupCommand.java\nDropCommand.java / ItemsCommand.java / MonsterAttackCommand.java'),
    ('repository', 'GameSaveRepository / RoomStateRepository / MonsterStateRepository\nBagItemRepository / ShopStateRepository'),
]
table = doc.add_table(rows=len(pkg_items) + 1, cols=2)
table.style = 'Table Grid'
table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(table, 0, ['包名', '主要内容'])
for idx, (pkg, desc) in enumerate(pkg_items):
    add_data_row(table, idx + 1, [pkg, desc])

doc.add_page_break()

# ============================================================
#               四、功能模块说明
# ============================================================
add_heading('四、功能模块说明', level=1)

# 4.1 随机地图生成
add_heading('4.1 随机地图生成', level=2)
add_para(
    '地图生成由 GenerateRoom.java 实现，采用程序化算法生成 10×15 网格（约 30-50 个房间）的地图。'
    '每次游戏使用不同的随机种子，确保每次体验独一无二。', indent=True
)
add_para('地图参数：', bold=True)
map_params = [
    '网格尺寸：10（宽）× 15（高）',
    '房间总数：约 30-50 个（视随机生成算法结果）',
    '出口方向：east（东）、west（西）、south（南）、north（北）',
    '传送房间：每局游戏随机标记 1 个传送房间',
    '7 种房间类型：START_HALL / SHOP / ENCOUNTER / CAMPFIRE / BOSS / ELITE_MONSTER / NORMAL_MONSTER',
]
for mp in map_params:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(mp)
    run.font.size = Pt(11)

# 4.2 实时战斗系统
add_heading('4.2 实时战斗系统', level=2)
add_para(
    '战斗系统采用"前端呈现 + 后端判定"模式。前端捕获玩家输入和怪物坐标快照，发送给后端进行空间命中判定。'
    '后端根据攻击类型执行不同的命中算法，返回命中结果和伤害数值。', indent=True
)
add_para('三种攻击模式：', bold=True)
attack_modes = [
    '扇形扫击（Sweep）：半径 120px、角度 135° 的扇形范围，以玩家朝向为中心。前端持续约 140ms，带残影渐隐特效。',
    '直线突刺（Pierce）：距离 120px、判定宽度 21px（半径），从起点到终点的线段距离判定。前端持续约 100ms，附带击退效果（55px/130ms）。',
    '蓄力攻击（Charged）：半径 150px、360° 全方向范围攻击，需长按蓄力 200ms 以上触发，附带屏幕振动。',
]
for am in attack_modes:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(am)
    run.font.size = Pt(11)

add_para(
    '攻击伤害计算：物理伤害 = 攻击方攻击力 - 防御方防御力（下限为 1）。'
    '魔法伤害通过 Magic.calcMagicDamage 方法计算。攻击命中后触发流血效果结算，'
    '被击败的怪物调用 processMonsterDrop 掉落货币。', indent=True
)

# 4.3 怪物系统
add_heading('4.3 怪物系统', level=2)
add_para(
    'Monster.java 定义了三种怪物类型：TYPE_NORMAL(0)、TYPE_ELITE(1)、TYPE_BOSS(2)。'
    '另有一个特殊怪物类型 FLAME_SLIME（火焰史莱姆），攻击附加烧伤效果。', indent=True
)
add_para('怪物生成规则（spawnMonsters）：', bold=True)
monster_rules = [
    'Boss 房间：生成 1 只 Boss 怪物，属性较高',
    '精英房间：生成 1-2 只精英怪物',
    '普通房间：生成 1-3 只普通怪物（固定 25% 概率生成 1 只火焰史莱姆代替普通怪物）',
    '所有怪物生成使用种子随机数，保证存档读档时怪物状态一致',
    '怪物房间的出口在全部怪物被击败前处于封锁状态',
]
for mr in monster_rules:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(mr)
    run.font.size = Pt(11)

# 4.4 状态效果系统
add_heading('4.4 状态效果系统', level=2)
add_para(
    'Status.java 实现了完整的 Buff/Debuff 状态系统，通过 StatusManager 统一管理。'
    '所有效果支持层数叠加（layers 机制），前端通过 getActiveEffectsInfo() 获取当前活跃效果列表。', indent=True
)

status_table = doc.add_table(rows=5, cols=5)
status_table.style = 'Table Grid'
status_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(status_table, 0, ['状态', '类型', '触发间隔', '伤害计算', '层数衰减'])
status_data = [
    ('BURN（烧伤）', '减益', '3秒', '魔法伤害 = 层数 × (1 - 魔抗/100)', '每轮减半'),
    ('POISON（中毒）', '减益', '1秒', '真实伤害 = min(层数, 当前HP-1)', '每轮减1'),
    ('BLEED（流血）', '减益', '攻击触发', '2点真实伤害', '每轮减1'),
    ('ANGEL_BUFF（天使祝福）', '增益', '持续30秒', '全属性×1.5倍', '定时结束'),
]
for idx, (name, dtype, interval, dmg, decay) in enumerate(status_data):
    add_data_row(status_table, idx + 1, [name, dtype, interval, dmg, decay])

# 4.5 背包与装备系统
add_heading('4.5 背包与装备系统', level=2)
add_para(
    '背包系统由 Bag.java 实现，支持添加、移除、使用和丢弃物品。getBackpackItems() 方法返回前端展示列表：'
    '消耗品按名称合并数量显示，装备和饰品每个独立占位。', indent=True
)
add_para(
    '装备系统由 Player.java 实现，支持 5 个装备槽位：武器（weapon）、护甲（armor）、披风（cloak）、'
    '戒指（ring）、项链（amulet）。通过 equipItem/unequipItem 方法管理装备，装备时自动卸下同槽位旧装备。', indent=True
)

equip_table = doc.add_table(rows=6, cols=4)
equip_table.style = 'Table Grid'
equip_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(equip_table, 0, ['装备名称', '槽位', '价格', '属性加成'])
equip_data = [
    ('铁剑', '武器', '80g', '物理攻击+15'),
    ('铁盾', '护甲', '70g', '物理防御+10'),
    ('暗影披风', '披风', '200g', '闪避+15%，速度+20'),
    ('生命戒指', '戒指', '150g', '最大HP+50，每2秒恢复1HP'),
    ('元素项链', '项链', '150g', '魔法攻击+15，魔抗+20'),
]
for idx, (name, slot, price, bonus) in enumerate(equip_data):
    add_data_row(equip_table, idx + 1, [name, slot, price, bonus])

# 4.6 商店系统
add_heading('4.6 商店系统', level=2)
add_para(
    'ShopCommand.java 实现商店买卖功能，仅在 SHOP 类型房间可用。商店随机从 ItemRegistry 选取 '
    '6 件商品上架，每件售出后标记为"sold"。出售价格 = 买价的一半（向下取整，最低 1g）。', indent=True
)

# 4.7 篝火祭坛系统
add_heading('4.7 篝火祭坛系统', level=2)
add_para(
    'InteractCommand.java 实现了篝火房间中的三种祭坛交互：', indent=True
)
altar_items = [
    '治愈祭坛（HEAL）：回复 50% HP 和 MP',
    '训练祭坛（TRAIN）：提升 25% 攻击/魔攻/防御，+10 魔抗',
    '博学祭坛（WISDOM）：展示 3 个随机 WisdomBoon 恩赐供玩家选择 1 个',
]
for a in altar_items:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(a)
    run.font.size = Pt(11)

add_para('8 种智慧恩赐（WisdomBoon）：', bold=True)
boon_table = doc.add_table(rows=9, cols=3)
boon_table.style = 'Table Grid'
boon_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(boon_table, 0, ['名称', '中文名', '效果'])
boon_data = [
    ('ENDURANCE', '坚忍', '提高30%血量上限'),
    ('VASTNESS', '浩瀚', '提高50%魔力上限'),
    ('PREPARATION', '整备', '提高15点物防'),
    ('GUARDIAN', '守护', '提高25点魔抗'),
    ('VIGOR', '强健', '提高15点物攻'),
    ('ERUDITION', '博学', '提高25点魔攻'),
    ('AGILITY', '敏捷', '提高50点移速'),
    ('GRACE', '灵动', '提高25点闪避率'),
]
for idx, (name, cn, effect) in enumerate(boon_data):
    add_data_row(boon_table, idx + 1, [name, cn, effect])

# 4.8 奇遇事件系统
add_heading('4.8 奇遇事件系统', level=2)
add_para(
    'RandomEventType.java 定义了 8 种随机事件：CHEST（宝箱200g）、MAIDEN（圣女满血回复）、'
    'ANGEL（天使30s全属性×1.5）、BLACKSMITH（铁匠装备强化150%）、WOODEN_CHEST（木箱75-150g）、'
    'GOLDEN_CHEST（金箱999g，25%概率替换木箱）、ELITE_ENEMY（精英敌人，击败后施加烧伤/中毒/流血5-10层）、'
    'FOUNTAIN（喷泉：50%扣10HP得20g，50%扣25g回10HP）。', indent=True
)
add_para(
    '奇遇分三档生成（roll 1-100）：优质（1-20，宝箱类）、劣质（21-50，精英敌人）、'
    '一般（51-100，喷泉）。旧版事件（CHEST/MAIDEN/ANGEL/BLACKSMITH）保留兼容。', indent=True
)

# 4.9 铁匠强化系统
add_heading('4.9 铁匠强化系统', level=2)
add_para(
    '铁匠事件（BLACKSMITH）中，玩家可让铁匠强化一件已装备的饰品或装备，属性提升至 150%。'
    '铁匠只接受等级较高的物品（暗影披风、生命戒指、元素项链），强化后原物品属性 ×1.5。'
    '该功能由熊俊森同学实现，包含保底机制避免强化失败。', indent=True
)

# 4.10 月光波技能
add_heading('4.10 月光波技能', level=2)
add_para(
    'WaveCommand.java 实现月光波技能：消耗 30 MP，对指定单体怪物造成魔法伤害。'
    '前端使用 Phaser 3 粒子系统实现投射物特效，弹射速度 420px/s，最多弹射 2 次，'
    '附带击退效果（40px/150ms）。2 秒窗口保护防止多次扣 MP。', indent=True
)

# 4.11 存档系统
add_heading('4.11 存档系统', level=2)
add_para(
    '存档系统基于 Spring Data JPA + H2 数据库实现，由 SaveService.java 提供完整 CRUD 操作。'
    '存档保存以下状态：玩家属性、背包物品列表、装备状态、当前房间及所有房间状态、怪物状态、'
    '商店商品状态。读档时从数据库加载并重建完整的游戏对象图。', indent=True
)
add_para('存档相关 API：', bold=True)
save_apis = [
    'POST /api/save — 保存游戏（可选 saveId 参数，不传则新建）',
    'POST /api/load — 加载存档（必传 saveId）',
    'GET /api/saves — 获取所有存档摘要列表',
    'POST /api/deleteSave — 删除指定 ID 的存档',
]
for sa in save_apis:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(sa)
    run.font.size = Pt(11)

doc.add_page_break()

# ============================================================
#               五、API 接口设计
# ============================================================
add_heading('五、API 接口设计', level=1)

add_para(
    '后端提供 10 个 RESTful API 端点，所有响应采用统一格式：'
    '{status: "success|error", message: "...", data: {...}}。', indent=True
)

api_table = doc.add_table(rows=11, cols=3)
api_table.style = 'Table Grid'
api_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(api_table, 0, ['方法', '路径', '说明'])
api_data = [
    ('GET', '/api/game', '获取当前游戏状态（含房间/玩家/背包/状态）'),
    ('POST', '/api/command', '执行游戏命令（go/bag/interact/shop/wave等）'),
    ('POST', '/api/attack', '执行玩家攻击（sweep/pierce/charged三种模式）'),
    ('POST', '/api/reset', '重置游戏，生成全新随机地图'),
    ('POST', '/api/save', '保存游戏到指定存档槽位'),
    ('POST', '/api/load', '从指定存档槽位加载游戏'),
    ('GET', '/api/saves', '获取所有存档的摘要列表'),
    ('POST', '/api/deleteSave', '删除指定 ID 的存档'),
    ('GET', '/api/map', '获取全地图拓扑数据（小地图渲染使用）'),
    ('GET', '/api/backpack', '获取背包物品列表（当前返回完整游戏状态）'),
]
for idx, (method, path, desc) in enumerate(api_data):
    add_data_row(api_table, idx + 1, [method, path, desc])

doc.add_paragraph()
add_para('支持的命令清单：', bold=True)
cmd_items = [
    'go <direction> — 向指定方向移动（east/west/south/north）',
    'help — 显示所有可用命令',
    'quit — 退出游戏',
    'take/pickup <itemName> — 拾取物品',
    'drop <itemName> — 丢弃物品',
    'items — 查看房间和背包物品信息',
    'bag use <itemName> — 使用消耗品或装备物品',
    'bag discard <itemName> — 从背包丢弃指定物品',
    'bag unequip <itemName> — 卸下已装备的物品',
    'shop buy <itemName> — 购买商品',
    'shop sell <itemName> — 出售背包物品',
    'interact heal/train/wisdom — 与祭坛交互',
    'interact wisdom <boonName> — 选择博学恩赐',
    'interact event — 触发奇遇事件/铁匠对话',
    'interact <itemName> — 铁匠选择强化装备',
    'wave <monsterName> — 月光波技能',
    'test poison/burn/bleed — 调试命令，施加状态效果',
]
for c in cmd_items:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(c)
    run.font.size = Pt(11)

doc.add_page_break()

# ============================================================
#               六、数据库设计
# ============================================================
add_heading('六、数据库设计', level=1)

add_para(
    '采用 H2 嵌入式数据库（文件模式），通过 Spring Data JPA 实现持久化。'
    '数据库文件位于项目 data/ 目录下。', indent=True
)

db_table = doc.add_table(rows=6, cols=2)
db_table.style = 'Table Grid'
db_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(db_table, 0, ['实体/Repository', '存储内容'])
db_data = [
    ('GameSaveEntity / GameSaveRepository', '存档元信息（玩家名、HP/MP/金钱、当前房间、时间戳）'),
    ('RoomStateEntity / RoomStateRepository', '所有房间的独立状态（出口、物品、怪物列表、事件状态等）'),
    ('MonsterStateEntity / MonsterStateRepository', '每个怪物的状态（HP/位置/类型/存活状态）'),
    ('BagItemEntity / BagItemRepository', '背包中每个物品的记录（名称/数量/装备状态）'),
    ('ShopStateEntity / ShopStateRepository', '商店中每个商品的售出状态'),
]
for idx, (entity, content) in enumerate(db_data):
    add_data_row(db_table, idx + 1, [entity, content])

doc.add_page_break()

# ============================================================
#               七、小组分工与协作
# ============================================================
add_heading('七、小组分工与协作', level=1)

add_para(
    '本项目由 AAA603 小组三位成员协作完成。开发过程中采用 GitHub 进行版本控制和任务管理，'
    '各成员在独立分支上开发，通过 Pull Request 进行代码审查与合并。', indent=True
)

member_table = doc.add_table(rows=4, cols=4)
member_table.style = 'Table Grid'
member_table.alignment = WD_TABLE_ALIGNMENT.CENTER
make_header_row(member_table, 0, ['姓名', 'GitHub', '分支', '主要贡献'])

member_data = [
    ('李冬晨\n（组长）',
     '@Sader-Lee\nldc分支',
     'ldc',
     '• 项目架构设计与搭建\n• 玩家系统（属性/装备/伤害计算）\n• take/drop/items 命令\n• 怪物系统（生成/攻击/掉落）\n• 商店系统（购买/出售）\n• 背包系统\n• 房间战斗封锁机制\n• 数据库持久化\n• 攻击特效与前后端联调'),
    ('蒋志成',
     '@BytesJiang\njzc分支',
     'jzc',
     '• 前端框架搭建与Phaser集成\n• WASD移动控制与视角\n• 小地图渲染\n• UI/UX 优化\n• 攻击特效（火焰粒子+屏幕振动）\n• 蓄力攻击与击退效果\n• 状态效果系统（烧伤/中毒/流血）\n• 随机地图生成算法\n• 篝火祭坛交互\n• 奇遇房间\n• 月光波技能\n• Bug 修复与性能优化'),
    ('熊俊森',
     '@XJS-123\nxjs分支',
     'xjs',
     '• 初始页面搭建设计\n• 饰品系统完整实现（5槽位装备）\n• 奇遇事件系统\n• 人物模型绘制\n• Buff系统（天使buff与中毒冲突处理）\n• 铁匠系统（保底机制）\n• 游戏实体构造（掉落物等）\n• 地图背景添加'),
]
for idx, (name, github, branch, contribution) in enumerate(member_data):
    row = member_table.rows[idx + 1]
    cells = row.cells
    # Name cell
    cells[0].text = ''
    p = cells[0].paragraphs[0]
    run = p.add_run(name)
    run.font.size = Pt(10)
    run.bold = True
    # GitHub cell
    cells[1].text = ''
    p = cells[1].paragraphs[0]
    run = p.add_run(github)
    run.font.size = Pt(10)
    # Branch cell
    cells[2].text = ''
    p = cells[2].paragraphs[0]
    run = p.add_run(branch)
    run.font.size = Pt(10)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    # Contribution cell
    cells[3].text = ''
    p = cells[3].paragraphs[0]
    run = p.add_run(contribution)
    run.font.size = Pt(10)

doc.add_paragraph()
add_para('协作流程：', bold=True)
collab_steps = [
    '任务拆分：通过 GitHub Issues 创建任务卡片，分配给对应成员',
    '分支开发：每个成员在独立分支（ldc/jzc/xjs）上开发',
    '代码审查：通过 Pull Request 进行交叉代码审查',
    '持续集成：配置 GitHub Actions 自动化构建与测试',
    '文档同步：README.md 和 API.md 随代码更新保持一致',
]
for cs in collab_steps:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(cs)
    run.font.size = Pt(11)

doc.add_page_break()

# ============================================================
#               八、实训总结与反思
# ============================================================
add_heading('八、实训总结与反思', level=1)

add_para('8.1 技术收获', bold=True)
tech_gains = [
    '深入理解了前后端分离架构的设计思想与实践方法，掌握 Spring Boot 与 Vue 3 的完整开发流程',
    '掌握了 Phaser 3 游戏引擎的基本用法，包括场景管理、碰撞检测、粒子系统、动画与特效',
    '学习了 Roguelike 游戏的程序化内容生成技术，包括随机地图生成、随机事件系统、平衡性设计',
    '实践了 RESTful API 设计规范，理解了统一响应格式、错误处理、状态管理的重要性',
    '掌握了 Spring Data JPA 的实体关系映射与复杂状态序列化/反序列化',
    '通过 Git 分支管理与代码审查，体验了真实团队协作的开发流程',
]
for tg in tech_gains:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(tg)
    run.font.size = Pt(11)

add_para('8.2 项目亮点', bold=True)
highlights = [
    '完整的空间命中判定系统：后端实现三种攻击模式（扇形/直线/全向）的几何命中算法，前端发送坐标快照做服务端校验',
    '丰富的状态效果系统：烧伤/中毒/流血/天使祝福均支持层数叠加与独立结算，前端实时展示状态图标与倒计时',
    '灵活的装备系统：五槽位设计支持武器/护甲/披风/戒指/项链的装备与卸下，属性加成实时反映到战斗面板',
    '多维度的内容生成：随机地图 + 随机商店 + 随机祭坛恩赐 + 随机奇遇事件，保证每次游戏体验的全新性',
    '完整的存档系统：基于 H2 数据库实现全量游戏状态持久化，支持多槽位存档管理',
]
for h in highlights:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(h)
    run.font.size = Pt(11)

add_para('8.3 遇到的挑战与解决方案', bold=True)
challenges = [
    ('前后端数据同步', '攻击判定涉及前端坐标与后端模型的同步问题。解决方案：前端发送坐标快照到后端，后端统一做空间判定，保证判定一致性。'),
    ('状态效果系统设计', '多种 Buff/Debuff 需要独立结算又可能同时存在。解决方案：使用 StatusManager 统一管理，采用层数叠加工厂模式，各效果独立 tick。'),
    ('随机种子一致性', '存档读档需要保证随机地图的可复现性。解决方案：使用种子随机数生成器，存档时保存种子值，读档时恢复种子重建地图。'),
    ('装备冲突处理', '同槽位装备切换时需卸下旧装备再装备新装备。解决方案：equipItem 方法自动检测同槽位并先调用 unequipSlot。'),
]
for title, solution in challenges:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(f'{title}：{solution}')
    run.font.size = Pt(11)

add_para('8.4 未来展望', bold=True)
future_items = [
    '增加更多怪物种类与 Boss 技能机制',
    '引入装备强化/镶嵌/附魔系统',
    '增加游戏内成就系统与排行榜',
    '优化 Phaser 性能，支持更多粒子效果与动画帧',
    '添加音效与背景音乐系统',
    '支持多人在线联机模式',
]
for fi in future_items:
    p = doc.add_paragraph(style='List Bullet')
    run = p.add_run(fi)
    run.font.size = Pt(11)

# ============================================================
#                       结 尾
# ============================================================
doc.add_paragraph()
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('— 报告完 —')
run.font.size = Pt(14)
run.font.color.rgb = RGBColor(0x99, 0x99, 0x99)

doc.add_paragraph()
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run(f'武汉理工大学 · 计算机科学与技术学院\n{datetime.date.today().strftime("%Y年%m月%d日")}')
run.font.size = Pt(11)
run.font.color.rgb = RGBColor(0x66, 0x66, 0x66)

# ========== 保存 ==========
output_path = 'REPORT.docx'
doc.save(output_path)
print(f'[OK] Report generated: {output_path}')
