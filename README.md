# AMA-android-sdk

This contains the SDK for Aries mobileagent android, an open source mobile agent for achieving self sovereign identity (SSI), created as part NGI-Trust eSSIF Lab, with efforts from iGrant.io, unikk.me, MyData etc. This SDK can be packaged into any mobile app enabling them with a decentralised datawallet App features. 


Download
--------

```gradle
repositories {
    google()
    mavenCentral()
    maven { url "https://jitpack.io" }
    maven { url 'https://repo.sovrin.org/repository/maven-public'}
}

dependencies {
    implementation 'com.github.decentralised-dataexchange:ama-android-sdk:1.12.3'

    implementation platform('com.google.firebase:firebase-bom:28.0.1')
    implementation 'com.google.firebase:firebase-dynamic-links-ktx'
}
```

Intializing
--------

````kotlin
DataWallet.initializeSdk(
            this,
            object : InitializeWalletCallback {
                override fun progressUpdate(progress: Int) {
                    when (progress) {
                        InitializeWalletState.INITIALIZE_WALLET_STARTED -> {
                            
                        }
                        InitializeWalletState.INITIALIZE_WALLET_EXTERNAL_FILES_LOADED -> {
                            
                        }
                        InitializeWalletState.POOL_CREATED -> {
                            
                        }
                        InitializeWalletState.WALLET_OPENED -> {
                           
                        }
                    }
                }
            },LedgerNetworkType.getSelectedNetwork(this)
        )
````

After Wallet has opened, we can start accessing the Wallet, Connection, Notification etc.

### Wallet
````
    DataWallet.showWallet(this)
````

### Connections
````
    DataWallet.showConnections(this)
````

### Notifications
````
    DataWallet.showNotifications(this)
````

## Licensing
Copyright (c) 2021 LCubed AB (iGrant.io), Sweden

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the LICENSE for the specific language governing permissions and limitations under the License.
