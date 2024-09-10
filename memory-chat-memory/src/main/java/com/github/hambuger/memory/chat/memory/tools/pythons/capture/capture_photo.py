import cv2


def capture_photo(filename):
    cap = cv2.VideoCapture(0)

    if not cap.isOpened():
        return


    ret, frame = cap.read()

    if not ret:
        cap.release()
        return


    cv2.imwrite(filename, frame)


    cap.release()
    cv2.destroyAllWindows()
