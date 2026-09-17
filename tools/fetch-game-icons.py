"""Build the offline pack from a pinned upstream revision; preserve original bytes."""
import concurrent.futures, hashlib, json, pathlib, subprocess, urllib.request, zipfile
ROOT = pathlib.Path(__file__).resolve().parents[1]
REV = 'd84d7fa19e546047b6d1acf6f00c14519da8c00f'
REPO = 'ClashKingInc/ClashKingAssets'
BASE = f'https://raw.githubusercontent.com/{REPO}/{REV}/'
groups = {
 'buildings/home-village': '1000000:army_camp 1000001:town_hall 1000002:elixir_collector 1000003:elixir_storage 1000004:gold_mine 1000005:gold_storage 1000006:barracks 1000007:laboratory 1000008:cannon 1000009:archer_tower 1000010:wall 1000011:wizard_tower 1000012:air_defense 1000013:mortar 1000014:clan_castle 1000015:builder\'s_hut 1000019:hidden_tesla 1000020:spell_factory 1000021:x-bow 1000023:dark_elixir_drill 1000024:dark_elixir_storage 1000026:dark_barracks 1000027:inferno_tower 1000028:air_sweeper 1000029:dark_spell_factory 1000031:eagle_artillery 1000032:bomb_tower 1000059:workshop 1000067:scattershot 1000068:pet_house 1000070:blacksmith 1000071:hero_hall 1000072:spell_tower 1000077:monolith',
 'buildings/builder-base': '1000034:builder_hall 1000035:elixir_collector 1000036:elixir_storage 1000037:gold_mine 1000038:gold_storage 1000039:clock_tower 1000040:builder_barracks 1000041:double_cannon 1000042:army_camp 1000043:hidden_tesla 1000044:cannon 1000045:multi_mortar 1000046:star_laboratory 1000048:archer_tower 1000050:firecrackers 1000051:guard_post 1000052:mega_tesla 1000054:air_bombs 1000055:crusher 1000056:roaster 1000057:giant_cannon 1000058:gem_mine 1000063:lava_launcher 1000065:bob_control 1000078:otto\'s_outpost 1000082:healing_hut',
 'heroes': '28000000:barbarian_king 28000001:archer_queen 28000002:grand_warden 28000003:battle_machine 28000004:royal_champion 28000005:battle_copter 28000006:minion_prince',
 'pets': '73000000:lassi 73000001:mighty_yak 73000002:electro_owl 73000003:unicorn 73000004:phoenix 73000007:poison_lizard 73000008:diggy 73000009:frosty 73000010:spirit_fox',
 'spells': '26000000:lightning_spell 26000001:healing_spell 26000002:rage_spell 26000003:jump_spell 26000005:freeze_spell 26000009:poison_spell 26000010:earthquake_spell 26000011:haste_spell 26000016:clone_spell 26000017:skeleton_spell 26000028:bat_spell 26000035:invisibility_spell 26000053:recall_spell 26000070:overgrowth_spell',
 'troops': '4000000:barbarian 4000001:archer 4000002:goblin 4000003:giant 4000004:wall_breaker 4000005:balloon 4000006:wizard 4000007:healer 4000008:dragon 4000009:pekka 4000010:minion 4000011:hog_rider 4000012:valkyrie 4000013:golem 4000015:witch 4000017:lava_hound 4000022:bowler 4000023:baby_dragon 4000024:miner 4000031:raged_barbarian 4000032:sneaky_archer 4000033:beta_minion 4000034:boxer_giant 4000035:bomber 4000036:power_pekka 4000037:cannon_cart 4000038:drop_ship 4000041:baby_dragon 4000042:night_witch 4000070:hog_glider 4000051:wall_wrecker 4000052:battle_blimp 4000053:yeti 4000058:ice_golem 4000059:electro_dragon 4000062:stone_slammer 4000065:dragon_rider 4000075:siege_barracks 4000082:headhunter 4000087:log_launcher 4000091:flame_flinger 4000095:electro_titan 4000097:apprentice_warden 4000123:druid'
}
def main():
 tree = json.loads(subprocess.check_output(['gh','api',f'repos/{REPO}/git/trees/{REV}?recursive=1']))
 paths = {e['path']:e['sha'] for e in tree['tree'] if e['type']=='blob'}
 selected = []
 for group, spec in groups.items():
  for pair in spec.split():
   ident, name = pair.split(':')
   prefix = f'assets/{group}/{name}'
   matches = [p for p in paths if (p.startswith(prefix+'/level_') and p.endswith('.webp'))] if group.startswith('buildings/') else [prefix+'.webp' if group=='spells' else prefix+'/icon.webp']
   for path in matches:
    if path not in paths: continue
    level = pathlib.PurePosixPath(path).stem.replace('level_','') if group.startswith('buildings/') else '0'
    if level.isdigit(): selected.append((f'{ident}_{level}.webp',path))
 cache=ROOT/'build/game-assets';cache.mkdir(parents=True,exist_ok=True)
 def download(item):
  key,path=item; f=cache/key
  if not f.exists():
   with urllib.request.urlopen(BASE+urllib.parse.quote(path,safe='/'),timeout=45) as response: data=response.read()
   f.write_bytes(data)
  data=f.read_bytes()
  assert hashlib.sha1(f'blob {len(data)}\0'.encode()+data).hexdigest()==paths[path],path
  assert data[:4]==b'RIFF' and data[8:12]==b'WEBP',path
  return key,path,data
 import urllib.parse
 with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: results=list(pool.map(download,selected))
 out=ROOT/'app/src/main/res/raw';out.mkdir(parents=True,exist_ok=True)
 provenance=[]
 with zipfile.ZipFile(out/'game_icons.zip','w',zipfile.ZIP_STORED) as archive:
  for key,path,data in sorted(results):
   info=zipfile.ZipInfo(key,(2026,9,17,0,0,0));archive.writestr(info,data)
   provenance.append({'file':key,'source':BASE+urllib.parse.quote(path,safe='/'),'sha256':hashlib.sha256(data).hexdigest()})
 (ROOT/'docs/game-assets.json').write_text(json.dumps({'repository':REPO,'revision':REV,'assets':provenance},indent=2)+'\n',encoding='utf-8')
 print(f'Packed {len(results)} verified original images; {(out/"game_icons.zip").stat().st_size} bytes')
if __name__=='__main__': main()
