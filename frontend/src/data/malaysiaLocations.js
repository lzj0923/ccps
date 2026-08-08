export const malaysiaLocations = [
  { state: 'Johor', areas: ['Johor Bahru', 'Batu Pahat', 'Kluang', 'Kota Tinggi', 'Kulai', 'Mersing', 'Muar', 'Pontian', 'Segamat', 'Tangkak'] },
  { state: 'Kedah', areas: ['Alor Setar', 'Baling', 'Bandar Baharu', 'Kubang Pasu', 'Kulim', 'Langkawi', 'Padang Terap', 'Pendang', 'Pokok Sena', 'Sik', 'Yan'] },
  { state: 'Kelantan', areas: ['Kota Bharu', 'Bachok', 'Gua Musang', 'Jeli', 'Kuala Krai', 'Machang', 'Pasir Mas', 'Pasir Puteh', 'Tanah Merah', 'Tumpat'] },
  { state: 'Melaka', areas: ['Melaka Tengah', 'Alor Gajah', 'Jasin', 'Malacca City'] },
  { state: 'Negeri Sembilan', areas: ['Seremban', 'Jempol', 'Jelebu', 'Kuala Pilah', 'Port Dickson', 'Rembau', 'Tampin'] },
  { state: 'Pahang', areas: ['Kuantan', 'Bentong', 'Bera', 'Cameron Highlands', 'Jerantut', 'Lipis', 'Maran', 'Pekan', 'Raub', 'Rompin', 'Temerloh'] },
  { state: 'Penang', areas: ['George Town', 'Barat Daya', 'Seberang Perai Selatan', 'Seberang Perai Tengah', 'Seberang Perai Utara'] },
  { state: 'Perak', areas: ['Ipoh', 'Bagan Datuk', 'Batang Padang', 'Hilir Perak', 'Hulu Perak', 'Kampar', 'Kerian', 'Kinta', 'Kuala Kangsar', 'Larut Matang dan Selama', 'Manjung', 'Muallim', 'Perak Tengah'] },
  { state: 'Perlis', areas: ['Kangar', 'Arau', 'Padang Besar'] },
  { state: 'Sabah', areas: ['Kota Kinabalu', 'Keningau', 'Kudat', 'Lahad Datu', 'Ranau', 'Sandakan', 'Semporna', 'Tawau'] },
  { state: 'Sarawak', areas: ['Kuching', 'Bintulu', 'Kapit', 'Miri', 'Mukah', 'Samarahan', 'Sarikei', 'Serian', 'Sibu', 'Sri Aman'] },
  { state: 'Selangor', areas: ['Shah Alam', 'Petaling Jaya', 'Ampang Jaya', 'Kajang', 'Klang', 'Selayang', 'Sepang', 'Subang Jaya'] },
  { state: 'Terengganu', areas: ['Kuala Terengganu', 'Besut', 'Dungun', 'Hulu Terengganu', 'Kemaman', 'Marang', 'Setiu'] },
  { state: 'Kuala Lumpur', areas: ['Bukit Bintang', 'Cheras', 'Kepong', 'Lembah Pantai', 'Segambut', 'Setapak', 'Titiwangsa'] },
  { state: 'Labuan', areas: ['Victoria', 'Batu Arang', 'Kiamsam', 'Layang-Layangan', 'Patau-Patau'] },
  { state: 'Putrajaya', areas: ['Putrajaya'] }
];

export const findStateByArea = (area) => malaysiaLocations.find(location => location.areas.includes(area))?.state || '';
