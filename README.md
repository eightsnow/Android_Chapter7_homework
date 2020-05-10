# Android_Chapter7_homework
第七章多媒体基础作业 VideoPlayer

所用Andriod Studio版本为3.4.1，gradle版本为5.1.1

完成了基础版本的所有要求

完成了pro版本的第一个要求：选中一个本地视频文件，可以在“打开方式”中选择使用自制播放器“VideoPlayer”

本APP是在实机(华为荣耀8)上测试的，由于在该实机的“手机相册”中不能调用第三方APP，所以pro版本的第二个要求不知道是否成功实现

APP使用说明:

第一次打开APP后会请求权限。赋予权限后，可以点击界面上的“扫描本地文件”按钮扫描本地视频，等待一段时间后即可获取本地视频列表，点击任意一个条目即可观看视频

之后打开APP会自动扫描文件

该APP可以打开mp4、mkv、rmvb、flv、wmv、mov、f4v、avi

只测试了mp4、mkv、flv三种格式，均可以成功播放

安装APK需要使用 adb install -t 命令



