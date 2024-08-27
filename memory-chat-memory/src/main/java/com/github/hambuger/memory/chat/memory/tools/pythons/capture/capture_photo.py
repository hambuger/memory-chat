import cv2


def capture_photo(filename):
    # 打开默认摄像头（通常是索引0）
    cap = cv2.VideoCapture(0)

    # 检查摄像头是否成功打开
    if not cap.isOpened():
        return

    # 读取摄像头画面
    ret, frame = cap.read()

    if not ret:
        cap.release()
        return

    # 保存照片到文件
    cv2.imwrite(filename, frame)

    # 释放摄像头资源
    cap.release()
    cv2.destroyAllWindows()
