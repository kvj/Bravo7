import os
import subprocess
import re
import shutil

print 'Hi korea!'
im_path = 'C:\Program Files\ImageMagick-6.7.2-Q16\convert.exe'
icon20 = '../icon20'
icon32 = '../icon32'
icon72 = '../icon72'
icon_source = 'C:\\Downloads\\.bin\\icons\\350-Mobile-Icons'
icon20_source = '\\iOS Icons\\White Icons\\Standard Size'
icon32_source = '\\Android Icons\\White\\32px Icons'
icon72_source = '\\Android Icons\\White\\48px Icons'
col_prefixes = {'green': 'g', 'blue': 'b', 'gray': 'gr', 'magenta': 'm', 'orange': 'o', 'red': 'r'}
if os.path.exists(icon20):
    shutil.rmtree(icon20)
if os.path.exists(icon32):
    shutil.rmtree(icon32)
if os.path.exists(icon72):
    shutil.rmtree(icon72)
os.mkdir(icon20)
os.mkdir(icon32)
os.mkdir(icon72)
colors  = os.listdir('.')
for col in colors:
    if os.path.isdir(col):
        #print 'Color: '+col
        icons = os.listdir(col)
        #print 'Icons:', icons
        print col, ': ['
        for icon in icons:
            m = re.match('^\d{3}-(\w+)\.png$', icon)
            if m:
                icon_name = m.group(1).lower()
                #print 'Icon:', icon_name, col_prefixes[col]
                print '\''+col_prefixes[col]+'-'+icon_name+'\','
                shutil.copyfile(icon_source+icon20_source+'\\'+icon, icon20+'\\'+col_prefixes[col]+'-'+icon_name+'.png');
                shutil.copyfile(icon_source+icon32_source+'\\'+icon, icon32+'\\'+col_prefixes[col]+'-'+icon_name+'.png');
                shutil.copyfile(icon_source+icon72_source+'\\'+icon, icon72+'\\'+col_prefixes[col]+'-'+icon_name+'.png');
                #code = subprocess.call([im_path, icon_source+icon20_source+'\\'+icon, '+level-colors', 'DarkGreen,', icon20+'\\'+col_prefixes[col]+'-'+icon_name])
                #print 'Code:', code
        print '],'
