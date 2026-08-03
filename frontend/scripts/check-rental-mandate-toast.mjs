import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
if (!source.includes('<AdminRentalMandateWorkspace v-else-if="currentId === \'adminRentalMandates\'" :page="page" @toast="page.showToast" />')) {
  throw new Error('AdminRentalMandateWorkspace must forward toast events to page.showToast');
}

console.log('Rental mandate toast wiring is present');
