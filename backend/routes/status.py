# routes/status.py : (cihazların son durumunu verir)

from flask import Blueprint, jsonify
from firebase_admin import db

# Blueprint tanımlanıyor
status_bp = Blueprint('status', __name__)

# /status/<oda> endpointi
@status_bp.route('/status/<room>', methods=['GET'])
def get_room_status(room):
    """
    Belirli bir odadaki cihazların son durumunu verir.
    """
    try:
        ref_path = f"commands/{room}"
        room_ref = db.reference(ref_path)
        room_data = room_ref.get()

        if room_data is None:
            return jsonify({"error": f"'{room}' odası için veri bulunamadı."}), 404

        return jsonify(room_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500