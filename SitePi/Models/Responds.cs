using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using SitePi.Models;

namespace SitePi.Models
{
    public class Responds
    {
        [Key]
        public int Id { get; set; }

        public int UserId { get; set; }
        public User User { get; set; }
        
        public int QuizId { get; set; }
        public Quiz Quiz { get; set; }
        
        public string Profile_1 { get; set; }

        public string Profile_2 { get; set; }

        public string Profile_3 { get; set; }
    
        public string Profile_4 { get; set; }

        public string Profile_5 { get; set; }

        public string Profile_6 { get; set; }

        public string Profile_7 { get; set; }

        public string Profile_8 { get; set; }

        public string Profile_9 { get; set; }
    }
}