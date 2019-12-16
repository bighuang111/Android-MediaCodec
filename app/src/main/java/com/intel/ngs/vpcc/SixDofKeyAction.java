package com.intel.ngs.vpcc;


import android.util.Log;

import static java.lang.StrictMath.abs;

public class SixDofKeyAction {

    private static final int VK_LEFT = 0x25;
    private static final int VK_RIGHT = 0x27;
    private static final int VK_UP = 0x26;
    private static final int VK_DOWN = 0x28;
    private static final int VK_ADD = 0x6B;
    private static final int VK_SUBTRACT = 0x6D;
    private static final int VK_NUMPAD9 = 0x69; 	// Move upward
    private static final int VK_NUMPAD3 = 0x63;     // Move downward
    private static final int VK_NUMPAD6 = 0x66;		// Move to the right
    private static final int VK_NUMPAD4 = 0x64; 	// Move to the left
    private static final int VK_NUMPAD8 = 0x68;		// Move forward
    private static final int VK_NUMPAD2 = 0x62;		// Move backward

    //private float[] myOldOrientation = new float[3];
    //private float[] myOldTranslation = new float[3];
    private float[] myCurrentPose = new float[6];

    private static float arcoreStep = (float) 0.01;
    private static float sensorStep = (float) 1.0;
    private static final int step = 15;
    private int[] oldTouchPose = new int[4];
    private int oldDistanceX = 0;
    private int oldDistanceY = 0;
    private int oldKeyAction = 0;
    private int oldFingers = 1;

    private int fingers = 1;

    public SixDofKeyAction(){

    };

    public void resetKeyActionByTouch()
    {
        oldTouchPose[0] = 0;
        oldTouchPose[1] = 0;
        oldTouchPose[2] = 0;
        oldTouchPose[3] = 0;

        oldDistanceX = 0;
        oldDistanceY = 0;
        oldKeyAction = 0;


    }

    public int getKeyActionByArcoreSensor(float[] myTranslation, float[] myOrientation, int orientation)
    {
        int poseChanged = 0;
        if(orientation ==1)
        {
            poseChanged = getKeyActionByArcoreSensor1(myTranslation, myOrientation, orientation);
        }
        else
        {
            poseChanged = getKeyActionByArcoreSensor0(myTranslation, myOrientation, orientation);
        }
        return poseChanged;
    }

    private int getKeyActionByArcoreSensor0(float[] myTranslation, float[] myOrientation,int orientation)
    {
        int poseChanged = 0;
        if (orientation == 0)
            myOrientation[1] += 15.0;

        if (myOrientation[0] - myCurrentPose[0] > sensorStep) {
            poseChanged = VK_RIGHT;
            myCurrentPose[0] += sensorStep;
        } else if (myOrientation[0] - myCurrentPose[0] < -sensorStep) {
            poseChanged = VK_LEFT;
            myCurrentPose[0] -= sensorStep;
        } else if (myOrientation[1] - myCurrentPose[1] > sensorStep) {
            poseChanged = VK_DOWN;
            myCurrentPose[1] += sensorStep;
        } else if (myOrientation[1] - myCurrentPose[1] < -sensorStep) {
            poseChanged = VK_UP;
            myCurrentPose[1] -= sensorStep;
        } else if (myOrientation[2] - myCurrentPose[2] > sensorStep) {
            poseChanged = VK_ADD;
            myCurrentPose[2] += sensorStep;
        } else if (myOrientation[2] - myCurrentPose[2] < -sensorStep) {
            poseChanged = VK_SUBTRACT;
            myCurrentPose[2] -= sensorStep;
        } else if (myTranslation[0] - myCurrentPose[3] > arcoreStep) {
            poseChanged = VK_NUMPAD6;
            myCurrentPose[3] += arcoreStep;
        } else if (myTranslation[0] - myCurrentPose[3] < -arcoreStep) {
            poseChanged = VK_NUMPAD4;
            myCurrentPose[3] -= arcoreStep;
        } else if (myTranslation[1] - myCurrentPose[4] > arcoreStep) {
            poseChanged = VK_NUMPAD9;
            myCurrentPose[4] += arcoreStep;
        } else if (myTranslation[1] - myCurrentPose[4] < -arcoreStep) {
            poseChanged = VK_NUMPAD3;
            myCurrentPose[4] -= arcoreStep;
        } else if (myTranslation[2] - myCurrentPose[5] > arcoreStep) {
            poseChanged = VK_NUMPAD2;
            myCurrentPose[5] += arcoreStep;
        } else if (myTranslation[2] - myCurrentPose[5] < -arcoreStep) {
            poseChanged = VK_NUMPAD8;
            myCurrentPose[5] -= arcoreStep;
        }
        else {
            poseChanged = 0;
        }

        return poseChanged;
    }

    private int getKeyActionByArcoreSensor1(float[] myTranslation, float[] myOrientation, int orientation)
    {
        int poseChanged = 0;

        if(orientation ==1)
            myOrientation[2] += 30.0;

        if (myOrientation[0] - myCurrentPose[0] > sensorStep) {
            poseChanged = VK_RIGHT;
            myCurrentPose[0] += sensorStep;
        } else if (myOrientation[0] - myCurrentPose[0] < -sensorStep) {
            poseChanged = VK_LEFT;
            myCurrentPose[0] -= sensorStep;
        } else if (myOrientation[1] - myCurrentPose[1] > sensorStep) {
            poseChanged = VK_ADD;
            myCurrentPose[1] += sensorStep;
        } else if (myOrientation[1] - myCurrentPose[1] < -sensorStep) {
            poseChanged = VK_SUBTRACT;
            myCurrentPose[1] -= sensorStep;
        } else if (myOrientation[2] - myCurrentPose[2] > sensorStep) {
            poseChanged = VK_DOWN;
            myCurrentPose[2] += sensorStep;
        } else if (myOrientation[2] - myCurrentPose[2] < -sensorStep) {
            poseChanged = VK_UP;
            myCurrentPose[2] -= sensorStep;
        } else if (myTranslation[0] - myCurrentPose[3] > arcoreStep) {
            poseChanged = VK_NUMPAD6;
            myCurrentPose[3] += arcoreStep;
        } else if (myTranslation[0] - myCurrentPose[3] < -arcoreStep) {
            poseChanged = VK_NUMPAD4;
            myCurrentPose[3] -= arcoreStep;
        } else if (myTranslation[1] - myCurrentPose[4] > arcoreStep) {
            poseChanged = VK_NUMPAD9;
            myCurrentPose[4] += arcoreStep;
        } else if (myTranslation[1] - myCurrentPose[4] < -arcoreStep) {
            poseChanged = VK_NUMPAD3;
            myCurrentPose[4] -= arcoreStep;
        } else if (myTranslation[2] - myCurrentPose[5] > arcoreStep) {
            poseChanged = VK_NUMPAD2;
            myCurrentPose[5] += arcoreStep;
        } else if (myTranslation[2] - myCurrentPose[5] < -arcoreStep) {
            poseChanged = VK_NUMPAD8;
            myCurrentPose[5] -= arcoreStep;
        }
        else {
            poseChanged = 0;
        }

        return poseChanged;
    }

    private void setTouchPose(int nPose)
    {

        switch (nPose)
        {
            case VK_RIGHT:
                myCurrentPose[0] += sensorStep ;
                break;
            case VK_LEFT:
                myCurrentPose[0] -= sensorStep ;
                break;
            case VK_DOWN:
                myCurrentPose[1] += sensorStep ;
                break;
            case VK_UP:
                myCurrentPose[1] -= sensorStep ;
                break;
            case VK_ADD:
                myCurrentPose[2] += sensorStep ;
                break;
            case VK_SUBTRACT:
                myCurrentPose[2] -= sensorStep ;
                 break;
            case VK_NUMPAD6:
                myCurrentPose[3] += arcoreStep ;
                break;
            case VK_NUMPAD4:
                myCurrentPose[3] -= arcoreStep ;
                break;
            case VK_NUMPAD9:
                myCurrentPose[4] += arcoreStep ;
                break;
            case VK_NUMPAD3:
                myCurrentPose[4] -= arcoreStep ;
                break;
            case VK_NUMPAD8:
                myCurrentPose[5] += arcoreStep ;
                break;
            case VK_NUMPAD2:
                myCurrentPose[5] -= arcoreStep ;
                break;

            default:
                break;
        }
    }

    public float[] getMyCurrentPose()
    {
        return myCurrentPose;
    }

    public int getKeyActionByTouch(int[] touch)
    {
        if(touch[2] == 0 && touch[3] == 0)
            fingers = 1;
        else fingers = 2;

        if(oldFingers != fingers) {
            oldFingers = fingers;
            resetKeyActionByTouch();
        }



        if(fingers == 1)
        {
            int keyAction = getKeyAction1(touch[0], touch[1]);
            if(checkKeyAction(keyAction)) {
                setTouchPose(keyAction);
                return keyAction;
            }
        }
        else if(fingers == 2)
        {
            int distanceX = abs(touch[0] - touch[2]);
            int distanceY = abs(touch[1] - touch[3]);
            int keyAction = getKeyAction2(touch[0], touch[1], touch[2], touch[3], distanceX, distanceY);

            if(checkKeyAction(keyAction)) {
                setTouchPose(keyAction);
                return keyAction;
            }
        }


        return 0;
    }

    private int getKeyAction1(int x, int y)
    {

        if(oldTouchPose[0] == 0 && oldTouchPose[1] == 0)
        {
            oldTouchPose[0] = x;
            oldTouchPose[1] = y;
            oldKeyAction = 0;
            return 0;
        }

        if(x - oldTouchPose[0] > step)
        {
            oldTouchPose[0] += step;
            //oldKeyAction = VK_LEFT;
            return VK_LEFT;
        }
        else if (x - oldTouchPose[0] < -step)
        {
            oldTouchPose[0] -= step;
            //oldKeyAction = VK_RIGHT;
            return VK_RIGHT;
        }
        else if (y - oldTouchPose[1] > step)
        {
            oldTouchPose[1] += step;
            //oldKeyAction = VK_UP;
            return VK_UP;
        }
        else if (y - oldTouchPose[1] < -step)
        {
            oldTouchPose[1] -= step;
            //oldKeyAction = VK_DOWN;
            return VK_DOWN;
        }

        return 0;
    }

    private int getKeyAction2(int x1, int y1, int x2, int y2, int distanceX, int distanceY)
    {

        if(oldTouchPose[2] == 0 && oldTouchPose[3] == 0)
        {
            oldTouchPose[0] = x1;
            oldTouchPose[1] = y1;
            oldTouchPose[2] = x2;
            oldTouchPose[3] = y2;
            oldDistanceX = distanceX;
            oldDistanceY = distanceY;
            oldKeyAction = 0;
            return 0;
        }

        if(x1 - oldTouchPose[0] > step && x2 - oldTouchPose[2] > step)
        {
            oldTouchPose[0] += step;
            oldTouchPose[2] += step;
            return VK_NUMPAD4;
        }
        else if (x1 - oldTouchPose[0] < -step && x2 - oldTouchPose[2] < -step)
        {
            oldTouchPose[0] -= step;
            oldTouchPose[2] -= step;
            return VK_NUMPAD6;
        }
        else if (y1 - oldTouchPose[1] > step && y2 - oldTouchPose[3] > step)
        {
            oldTouchPose[1] += step;
            oldTouchPose[3] += step;
            return VK_NUMPAD9;
        }
        else if (y1 - oldTouchPose[1] < -step && y2 - oldTouchPose[3] < -step)
        {
            oldTouchPose[1] -= step;
            oldTouchPose[3] -= step;
            return VK_NUMPAD3;
        }
        else if (distanceX - oldDistanceX > 2* step && distanceY - oldDistanceY > 2* step)
        {
            oldDistanceX += 2*step;
            oldDistanceY += 2*step;
            return VK_NUMPAD8;
        }
        else if (distanceX - oldDistanceX < -2 * step && distanceY - oldDistanceY < -2* step)
        {
            oldDistanceX -= 2*step;
            oldDistanceY -= 2*step;
            return VK_NUMPAD2;
        }
        else if (distanceX - oldDistanceX < step && abs(y2 - oldTouchPose[3]) > 2* step)
        {
            int keyAction = 0;
            if(y2 > oldTouchPose[3])
            {
                keyAction = VK_ADD;
            }
            else
            {
                keyAction = VK_SUBTRACT;
            }

            oldTouchPose[3] = y2;
            return keyAction;
        }


        return 0;
    }

    private boolean checkKeyAction(int keyAction)
    {

        if(keyAction == oldKeyAction)
            return true;

        //when keyAction is not same as old, it will be stored.
        oldKeyAction = keyAction;


        if(keyAction == VK_LEFT && oldKeyAction == VK_RIGHT)
            return true;
        else if(keyAction == VK_RIGHT && oldKeyAction == VK_LEFT)
            return true;
        else if(keyAction == VK_UP && oldKeyAction == VK_DOWN)
            return true;
        else if(keyAction == VK_DOWN && oldKeyAction == VK_UP)
            return true;
        else if(keyAction == VK_NUMPAD2 && oldKeyAction == VK_NUMPAD8)
            return true;
        else if(keyAction == VK_NUMPAD8 && oldKeyAction == VK_NUMPAD2)
            return true;
        else if(keyAction == VK_NUMPAD4 && oldKeyAction == VK_NUMPAD6)
            return true;
        else if(keyAction == VK_NUMPAD6 && oldKeyAction == VK_NUMPAD4)
            return true;
        else if(keyAction == VK_NUMPAD3 && oldKeyAction == VK_NUMPAD9)
            return true;
        else if(keyAction == VK_NUMPAD9 && oldKeyAction == VK_NUMPAD3)
            return true;
        else if(keyAction == VK_ADD && oldKeyAction == VK_SUBTRACT)
            return true;
        else if(keyAction == VK_SUBTRACT && oldKeyAction == VK_ADD)
            return true;

        return false;
    }
}