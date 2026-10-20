BIT240136 - FOODMANAGER | HUONG DAN MO PROJECT VA NOP BAI
====================================================

1. MO PROJECT
- Giai nen file ZIP.
- Android Studio > File > Open > chon THU MUC BIT240136_FoodManager2.
- Cho Gradle Sync xong, chon emulator Pixel 7, nhan Run.
- KHONG uninstall ung dung cu / Clear Storage neu muon giu du lieu SQLite tren Pixel 7.
- applicationId van la com.example.foodmanager240136. Neu cai de len ung dung debug
  duoc ky bang keystore khac, Android co the tu choi cap nhat; sao luu du lieu truoc khi uninstall.

2. DA CO NHUNG CHUC NANG GI
- SQLite: bang BIT240136_DishCategory, BIT240136_Dish, BIT240136_DishImage.
- CRUD mon an, validate gia > 0, thoi gian > 0, ten khong rong, ten khong trung
  trong cung loai; trang thai ban, Quick / Regular.
- Tim kiem, loc loai / trang thai / khoang gia, sap xep gia va thoi gian.
- Quan ly loai mon; tu choi xoa loai mon dang co mon an.
- Quan ly nhieu anh bang HTTPS ImageUrl, chon 1 anh dai dien, xoa co xac nhan.
- Anh dai dien hien trong danh sach va chi tiet neu URL hoat dong.
- Chuyen EN/VI bang nut 'Language: EN / VI' tren man hinh chinh.
- SQLite queue luu cac thao tac khi offline; job sync doi ket noi mang va gui len MockAPI.

3. CAU HINH MOCKAPI - BAT BUOC DE DEMO PHAN ONLINE
A. Vao https://mockapi.io, tao project cua ban; lay base URL dang
   https://xxxxxxxx.mockapi.io/api/v1
B. Trong project MockAPI tao 3 resource, dung CHINH XAC ten:
   categories
   dishes
   images
C. Cac resource cho phep GET, POST, PUT, DELETE. MockAPI co the luu JSON field
   tu dong khi gui request. Neu phai khai bao schema, them 'BIT240136_LocalId'
   (number) va cac field BIT240136_... tuong ung bang SQLite.
D. Tren man hinh FoodManager nhan 'MockAPI Settings', nhap BASE URL
   (KHONG them /dishes hay /categories phia sau), nhan Save.
E. Kiem tra ket noi internet; nhan MockAPI Settings > Sync Now neu can.
F. Mo 3 resource tren MockAPI de kiem tra du lieu.

Huong dong bo hien tai: SQLite local -> MockAPI (1 chieu). App KHONG tu dong
lay va ghi de du lieu tu MockAPI ve SQLite, khong merge xung dot da thiet bi.
Khi offline, Queue giu thao tac den khi co ket noi. JobScheduler tu choi
network neu chua co mang; Android co the tre lich nen khong bao dam sync ngay
lap tuc. Neu API loi / sai endpoint, hang doi van giu, job se thu lai.
Chua co URL MockAPI that, nen khong the xac nhan remote sync tren server.

4. TEST MAT MANG
- Mo FoodManager, vao MockAPI Settings cau hinh URL truoc.
- Tat Wi-Fi va mobile network tren emulator.
- Them / sua / xoa mot mon TEST, mo MockAPI Settings kiem tra pending count.
- Bat lai Internet, doi mot chut theo lich Android.
- Mo MockAPI resource, doi chieu du lieu vua sua.
- Neu can, bam 'Sync Now' de dat lai job.

5. KIEM THU CAN LAM TREN ANDROID STUDIO
- Them mon: nhap name, price, time, category, description, availability.
- Validation: price=0, time=0, ten rong, trung ten/cung loai.
- Sua mon, dong form, kiem tra quay ve chi tiet va danh sach.
- Xoa mon: Cancel -> van con; Delete -> mat khoi danh sach.
- Loc gia 20000-40000, category Drinks, sort Low to High.
- Them 2 URL anh HTTPS that, chon anh dai dien khac, xoa anh.
- Thu EN/VI o man hinh danh sach/chi tiet/them mon/quan ly loai.
- Thu xoa loai dang duoc tham chieu va loai chua su dung.
- Thu offline queue + online MockAPI khi da tao resource.

6. QUAY VIDEO
Video 1: demo cac tinh nang cua app va cac truong hop validate.
Video 2: gioi thieu cau truc project, 3 bang SQLite, DatabaseHelper,
         Activity, queue, JobScheduler, vi du code them/sua/xoa, UI 2 ngon ngu.
Luu y: kiem tra lai yeu cau thoi luong va dinh dang video trong de giao vien.

7. QUAN TRONG
Project da duoc kiem tra XML va tham chieu tai nguyen bang script.
Khong the build Android tren may xu ly nay do chua co Android SDK / Gradle 9.6;
CAN mo Android Studio, Rebuild Project va Run de kiem chung truoc khi nop.
Khong duoc tu nhan da demo/mockapi thanh cong neu chua test tren mockapi.io.
