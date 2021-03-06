import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'asset',
        loadChildren: () => import('./asset/asset.module').then(m => m.DefiAssetModule)
      },
      {
        path: 'trusted-device',
        loadChildren: () => import('./trusted-device/trusted-device.module').then(m => m.DefiTrustedDeviceModule)
      },
      {
        path: 'wallet',
        loadChildren: () => import('./wallet/wallet.module').then(m => m.DefiWalletModule)
      },
      {
        path: 'transaction',
        loadChildren: () => import('./transaction/transaction.module').then(m => m.DefiTransactionModule)
      },
      {
        path: 'account-balance',
        loadChildren: () => import('./account-balance/account-balance.module').then(m => m.DefiAccountBalanceModule)
      },
      {
        path: 'address-book',
        loadChildren: () => import('./address-book/address-book.module').then(m => m.DefiAddressBookModule)
      }
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ])
  ],
  declarations: [],
  entryComponents: [],
  providers: [],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DefiEntityModule {}
