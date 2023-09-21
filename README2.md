## 背景说明

httpdns android sdk 是开源项目，
github 地址是 ： https://github.com/aliyun/alibabacloud-httpdns-android-sdk

同时也在公司内源开源
地址是 ： https://ach.alibaba-inc.com/detail/4098
仓库地址是 https://code.aone.alibaba-inc.com/alicloud-ams/alicloud-android-sdk-httpdns_for_github

由于合规问题，所以外部开源的代码和真正维护的代码是有些区分的，这些区分是通过分支区分的。

真正的开发仓库是 https://code.aone.alibaba-inc.com/alicloud-ams/alicloud-android-sdk-httpdns_for_open_source

而最初的开发仓库 https://code.aone.alibaba-inc.com/alicloud-ams/alicloud-android-sdk-httpdns 废弃不在使用


## 分支说明

remote 说明
```
aliyun	git@gitlab.alibaba-inc.com:alicloud-ams/alicloud-android-sdk-httpdns_for_github.git (fetch)
aliyun	git@gitlab.alibaba-inc.com:alicloud-ams/alicloud-android-sdk-httpdns_for_github.git (push)
github	git@github.com:aliyun/alibabacloud-httpdns-android-sdk.git (fetch)
github	git@github.com:aliyun/alibabacloud-httpdns-android-sdk.git (push)
origin	git@gitlab.alibaba-inc.com:alicloud-ams/alicloud-android-sdk-httpdns_for_open_source.git (fetch)
origin	git@gitlab.alibaba-inc.com:alicloud-ams/alicloud-android-sdk-httpdns_for_open_source.git (push)
```
aliyun 是 内源仓库
github 是 开源仓库
origin 是 开发仓库

分支关系是
origin/master <-- origin/develop <-- origin/dev_xxx --> aliyun/master github/master

开发过程的分支变化是
origin/dev_xxx 基于 开源代码拉取，push到开发仓库
开发完成 merge 到 origin/develop分支 提测
测试完成 origin/develop merge 到 origin/master； origin/dev_xxx merge 到 aliyun/master github/master 开源

分支的代码区分是
origin/develop 分支 增加了 打包配置和内部使用的ip配置，而开源的代码没有。



